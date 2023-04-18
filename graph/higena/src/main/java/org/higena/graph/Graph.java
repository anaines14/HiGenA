package org.higena.graph;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import io.github.cdimascio.dotenv.Dotenv;
import org.higena.A4FParser;
import org.higena.ast.Parser;
import org.higena.ast.TED;
import org.higena.graph.hint.Hint;
import org.higena.graph.hint.HintGenType;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper class for the database (Db class).
 */
public class Graph {
  private final String uri, user, password, databaseName, challenge, predicate;
  private final Parser parser;

  public Graph(String challenge, String predicate) {
    Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();

    this.uri = dotenv.get("NEO4J_URI");
    this.user = dotenv.get("NEO4J_USERNAME");
    this.password = dotenv.get("NEO4J_PASSWORD");
    this.challenge = challenge;
    this.predicate = predicate;
    this.databaseName = genDatabaseName(challenge, predicate);
    this.parser = new Parser(challenge);

    // Connect to the default database
    try (Db db = new Db(uri, user, password, challenge, predicate)) {
      db.verifyConnection();
      // Create database for this challenge and predicate if it does not exist
      db.addDb(this.databaseName);
    }
  }

  /**
   * Generates a databaseName accepted by neo4j. It removes all digits from the
   * challenge name, converts it to lowercase and takes the first 4 characters.
   * Then, if the predicate contains digits, it transforms them into letters using
   * the remainder of the division by 26 of the number (e.g 0 -> a, 1 -> b, etc.)
   * Otherwise, it just appends the predicate.
   *
   * @param challenge Challenge name
   * @param predicate Predicate name
   * @return Database name
   */
  private String genDatabaseName(String challenge, String predicate) {
    // remove all digits, convert to lowercase and take the first 4 characters
    StringBuilder ret = new StringBuilder(challenge.replaceAll("\\d", "").toLowerCase().substring(0, 4));
    // If predicate contains digits, transform them into letters
    if (predicate.matches(".*\\d.*")) {

      String[] numbers = predicate.replaceAll("[^0-9]", "").split("");
      for (Iterator<String> it = Arrays.stream(numbers).iterator(); it.hasNext(); ) {
        String number = it.next();
        ret.append((char) (Integer.parseInt(number) + 97));
      }
    } else {
      // If predicate does not contain digits, just append it
      ret.append(predicate);
    }
    return ret.toString();
  }

  /**
   * Sets up the graph database.
   */
  public void setup() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      System.out.println("Starting DB setup...");
      long startTime = System.currentTimeMillis();
      db.setup();
      long endTime = System.currentTimeMillis() - startTime;
      System.out.println("Success: Finished setup in " + endTime + " ms.");
    }
  }

  // Hint methods

  /**
   * Parses the given expression and returns the node from the database with
   * the same AST. If it does not exist, it searches for the most similar node
   * and creates a new node with the given expression and adds an edge between
   * these two nodes.
   *
   * @param db   Database instance.
   * @param expr Expression
   * @param code Code
   * @return Node from the database with the given expression.
   */
  private Node getSourceNode(Db db, String expr, String code) {
    // Parse expression
    String ast = parser.parse(expr, code);

    // Get node from database with the AST
    Node node = db.getNodeByAST(ast);

    if (node == null) { // If it does not exist, create it
      // Get the most similar node
      Node similarNode = db.getMostSimilarNode(ast);
      if (similarNode != null) {
        // Create the node with the AST
        Node n = db.addIncorrectNode(expr, ast, code);
        // Add edge between the new node and the most similar node
        db.addEdge(n, similarNode);
        return n;
      }
    }
    return node;
  }

  public Hint getHint(String expr, HintGenType type) {
    return getHint(expr, "", type);
  }

  public Hint getHint(String expr, String code, HintGenType type) {
    Hint hint = null;
    long startTime = System.currentTimeMillis();
    hint = switch (type) {
      case TED -> getDijkstraHint(expr, "ted", code);
      case REL_POISSON -> getDijkstraHint(expr, "poisson", code);
      case NODE_POISSON -> getDijkstraHint(expr, "dstPoisson", code);
    };
    long endTime = System.currentTimeMillis() - startTime;
    System.out.println("Success: Finished hint gen in " + endTime + " ms.");
    return hint;
  }

  /**
   * Finds the node with the given AST or creates it if it does not exist and
   * calculates the shortest path using the dijkstra algorithm to a Correct
   * node using the given property as edge weight. Then, it returns the
   * first edge of the path.
   *
   * @param expr     Expression of the node to find the hint for.
   * @param property Weight property to use in the dijkstra algorithm.
   * @return The first edge of the shortest path.
   */
  private Hint getDijkstraHint(String expr, String property, String code) {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      // Get the node to start the path from
      Node source_node = getSourceNode(db, expr, code);
      if (source_node == null) { // Failed to generate hint because of no
        // source node
        return null;
      }
      System.out.println("Hint for:\t" + source_node.get("expr").toString() +
              "\tAST: " + source_node.get("ast").toString());
      // Get the shortest path from the node to the goal node
      Result res = db.dijkstra(source_node.get("id").asString(), property);

      try {
        // Get results from the query to generate the hint
        Record rec = res.single();
        List<Node> nodes = rec.get("path").asList(Value::asNode);
        Relationship firstRel = db.getRelationship(nodes.get(0), nodes.get(1));
        Node dstNode = nodes.get(nodes.size() - 1), nextNode = nodes.get(1);

        return genHint(firstRel, source_node, dstNode, nextNode);

      } catch (NoSuchRecordException e) {
        // No path found
        // Find most similar correct node
        Node similarNode =
                db.getMostSimilarNode(source_node.get("ast").toString(),
                        "Correct");

        if (similarNode != null) {
          // Create edge between the two nodes
          Relationship hint_rel = db.addEdge(source_node, similarNode);
          return genHint(hint_rel, source_node, similarNode);
        } else {
          System.err.println("Error: Cannot generate hint.");
        }
      }
    }
    return null;
  }

  private Hint genHint(Relationship edge, Node source, Node solution) {
    return genHint(edge, source, solution, solution);
  }

  private Hint genHint(Relationship edge, Node source, Node solution,
                       Node next) {
    // Log
    System.out.println("Correct:\t" + solution.get("expr").toString() +
            "\tAST" +
            ": " + solution.get("ast").toString());
    System.out.println("Next:\t\t" + next.get("expr").toString() + "\tAST: " +
            next.get("ast").toString());
    System.out.println("Edit Operations:\t" + edge.get("operations").toString());

    // Calculate TED between the first and last node
    TED ted = new TED();
    int t = ted.computeEditDistance(source.get("ast").toString(),
            solution.get("ast").toString());

    return new Hint(t, edge);
  }

  // Auxiliar methods

  /**
   * Returns statistics for the current database (number of nodes, edges,
   * correct nodes, incorrect nodes).
   *
   * @return Record with the statistics.
   */
  public Record getStatistics() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      return db.getStatistics().single();
    }
  }

}

package org.higena.graph;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import io.github.cdimascio.dotenv.Dotenv;
import org.higena.A4FExprParser;
import org.higena.A4FParser;
import org.higena.ast.TED;
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
  private final CompModule challengeModule;

  public Graph(String challenge, String predicate) {
    Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();

    this.uri = dotenv.get("NEO4J_URI");
    this.user = dotenv.get("NEO4J_USERNAME");
    this.password = dotenv.get("NEO4J_PASSWORD");
    this.challenge = challenge;
    this.predicate = predicate;
    this.databaseName = genDatabaseName(challenge, predicate);
    this.challengeModule = CompUtil.parseEverything_fromFile(new A4Reporter(), null, "src/main/resources/challenges/" + challenge + ".als");

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
    // Get node from database with the AST
    String ast;
    if (expr.isEmpty())
      ast = "";
    else {
      try {
        ast = parseExpr(expr);
      } catch (Exception e) {
        try {
          ast = parseExpr(expr, code);
        } catch (Exception ex) {
          System.err.println("Error parsing expression: " + expr);
          return null;
        }
      }
    }
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
      default -> null;
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
  private Hint getDijkstraHint(String expr, String property,
                               String code) {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      // Get the node to start the path from
      Node node = getSourceNode(db, expr, code);
      if (node == null) { // Failed to generate hint because of no source node
        return null;
      }
      System.out.println("Incorrect: " + node.get("expr").toString() + "\tAST: " + node.get("ast").toString());
      // Get the shortest path from the node to the goal node
      Result res = db.dijkstra(node.get("id").asString(), property);
      try {
        // Get results from the query to generate the hint
        Record rec = res.single();
        List<Node> nodes = rec.get("path").asList(Value::asNode);
        Relationship firstRel = db.getRelationship(nodes.get(0), nodes.get(1));

        System.out.println("Correct: " + nodes.get(nodes.size() - 1).get(
                "expr").toString() + "\tAST: " + nodes.get(nodes.size() - 1).get("ast").toString());
        System.out.println("Next: " + nodes.get(1).get("expr").toString() + "\tAST: " + nodes.get(1).get("ast").toString());
        System.out.println("Edit Operations: " + firstRel.get("operations").toString());

        // Calculate TED between the first and last node
        TED ted = new TED();
        int t = ted.computeEditDistance(nodes.get(0).get("ast").toString(),
                nodes.get(nodes.size() - 1).get("ast").toString());

        return new Hint(t, firstRel);

      } catch (NoSuchRecordException e) {
        System.err.println("ERROR: Cannot retrieve hint.");
      }
    }
    return null;
  }

  // Parse functions

  /**
   * Extracts the expression from the full code. If the expression exists, it
   * parses it against the challenge module. If the parsing fails, it parses it
   * against the full module code passed as a parameter.
   *
   * @param fullCode The full module code.
   * @return The AST of the parsed expression.
   */
  private String parseExprFromCode(String fullCode) {
    A4FExprParser exprParser = new A4FExprParser(fullCode);
    String ast = null, expr = exprParser.parse(this.predicate);

    try {
      ast = parseExpr(expr);
    } catch (Exception e) {
      try {
        ast = parseExpr(expr, fullCode);
      } catch (Exception ex) {
        System.err.println("ERROR: cannot parse expression.");
      }
    }
    return ast;
  }

  /**
   * Parses an Alloy expression using the challenge module and returns the AST
   * of the parsed expression.
   *
   * @param expr The expression to parse.
   * @return The AST of the parsed expression.
   */
  public String parseExpr(String expr) {
    if (expr.equals("")) {
      return "";
    }
    return A4FParser.parse(expr, this.challengeModule).toString();
  }

  /**
   * Parses an Alloy expression using the full module code and returns the AST of the parsed expression.
   *
   * @param expr The expression to parse.
   * @param code The full module code.
   * @return The AST of the parsed expression.
   */
  private String parseExpr(String expr, String code) {
    if (expr.equals("")) {
      return "";
    }
    return A4FParser.parse(code, expr).toString();
  }

  // Auxiliar methods

  /**
   * Auxiliar method to run a query.
   *
   * @param query query stings.
   * @return list of records.
   */
  public List<Record> runQuery(String query) {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      return db.runQuery(query).list();
    }
  }

  /**
   * Print statistics for the current database (number of nodes, edges, correct nodes, incorrect nodes).
   */
  public void printStatistics() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      Result res = db.getStatistics();
      try {
        Record rec = res.single();
        System.out.printf("""
                Number of nodes: %d
                Number of edges: %d
                Number of correct nodes: %d
                Number of incorrect nodes: %d
                """, rec.get("submissions").asInt(), rec.get("derivations").asInt(), rec.get("corrects").asInt(), rec.get("incorrects").asInt());
      } catch (NoSuchRecordException e) {
        System.err.println("ERROR: Cannot retrieve statistics.");
      }
    }
  }

}

package org.higena.graph;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import io.github.cdimascio.dotenv.Dotenv;
import org.higena.A4FExprParser;
import org.higena.A4FParser;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.List;

/**
 * Wrapper class for the database (Db class).
 */
public class Graph {
  private final String uri, user, password, databaseName, challenge, predicate;
  private final CompModule challengeModule;

  public Graph() {
    Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();

    this.uri = dotenv.get("NEO4J_URI");
    this.user = dotenv.get("NEO4J_USERNAME");
    this.password = dotenv.get("NEO4J_PASSWORD");
    this.databaseName = "ajpkk";
    this.challenge = "9jPK8KBWzjFmBx4Hb";
    this.predicate = "prop1";
    this.challengeModule = CompUtil.parseEverything_fromFile(new A4Reporter(), null, "src/main/resources/challenges/" + challenge + ".als");

    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      db.verifyConnection();
    }
  }

  /**
   * Creates a new database and sets up the graph.
   */
  public void setup() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      System.out.println("Starting DB setup...");
      db.setup();
      System.out.println("Success: Finished setup.\n");
    }
  }

  public void getDijkstraHint(String ast) {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      // Get the node corresponding to the AST
      Node node = db.getNodeByAST(ast);
        // Get the shortest path from the node to the goal node
      Result res = db.dijkstra(node.get("id").asString());
      try {
        // Get the two first nodes in the path
        List<Node> rels = res.single().get("path").asList(Value::asNode);
        Node n1 = rels.get(0), n2 = rels.get(1);
        // Get the relationship between the two nodes
        Relationship rel = db.getRelationship(n1, n2);

        System.out.println("Edge ID: " + rel.get("id"));
        System.out.println("Operations: " + rel.get("operations"));
        System.out.println("TED: " + rel.get("ted"));

      } catch (NoSuchRecordException e) {
        System.out.println("ERROR: Cannot retrieve hint.");
      }
    }
  }

  /**
   * Extracts the expression from the full code. If the expression exists, it
   * parses it against the challenge module. If the parsing fails, it parses it
   * against the full module code passed as a parameter.
   * @param fullCode The full module code.
   * @return The AST of the parsed expression.
   */
  public String parseExprFromCode(String fullCode) {
    A4FExprParser exprParser = new A4FExprParser(fullCode);
    String ast = null, expr = exprParser.parse(this.predicate);

    if (!expr.equals("")) {
      try {
        ast = parseExpr(expr);
      } catch (Exception e) {
        try {
          ast = parseExpr(expr, fullCode);
        } catch (Exception ex) {
          System.out.println("ERROR: cannot parse expression.");
        }
      }
    }
    return ast;
  }

  /**
   * Parses an Alloy expression using the challenge module and returns the AST
   * of the parsed expression.
   *
   * @param expr            The expression to parse.
   * @return The AST of the parsed expression.
   */
  public String parseExpr(String expr) {
    return A4FParser.parse(expr, this.challengeModule).toString();
  }

  /**
   * Parses an Alloy expression using the full module code and returns the AST of the parsed expression.
   *
   * @param expr The expression to parse.
   * @param code The full module code.
   * @return The AST of the parsed expression.
   */
  public String parseExpr(String expr, String code) {
    return A4FParser.parse(code, expr).toString();
  }
}

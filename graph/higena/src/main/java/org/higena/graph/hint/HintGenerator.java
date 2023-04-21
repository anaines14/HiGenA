package org.higena.graph.hint;

import org.higena.graph.Db;
import org.json.JSONObject;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.List;

public class HintGenerator {

  private final Db db; // Database connection
  private final String expression, code; // Student submission
  private final HintGenType type; // Hint generation type
  // Hint generation data
  private boolean isNewNode; // True if submission is new on the graph
  private Node sourceNode; // Node from the graph with the same AST as the submission
  private Node targetNode; // Closest solution in the graph
  private Node nextNode; // Next node in the path to the solution
  private Relationship first_edge; // First edge in the path to the solution
  private double totalTED; // Total cost of the path to the solution
  private Hint hint; // Generated hint
  private long time; // Time it took to generate the hint

  public HintGenerator(String expression, String code, HintGenType type, Db db) {
    this.db = db;
    this.expression = expression;
    this.code = code;
    this.type = type;
  }

  /**
   * Finds the node with the given AST or creates it if it does not exist and
   * calculates the shortest path using the dijkstra algorithm to a Correct
   * node using the given property as edge weight. If no path to a correct node
   * exists, it creates a path to the most similar correct node. Then, it
   * returns the first edge of the path.
   *
   * @param ast AST of the expression to find the hint for.
   */
  public void generateHint(String ast) {
    // Start timer
    long startTime = System.currentTimeMillis();
    // Get source node
    sourceNode = getSourceNode(ast);
    if (sourceNode == null) { // cannot generate hint without source node
      return;
    }
    // Get the shortest path to a solution
    Result res = db.dijkstra(sourceNode.get("id").asString(), type.toString());

    try {
      Record rec = res.single();
      List<Node> nodes = rec.get("path").asList(Value::asNode);
      targetNode = nodes.get(nodes.size() - 1);
      nextNode = nodes.get(1);
      first_edge = db.getRelationship(sourceNode, nextNode);
      if (type == HintGenType.TED)
        totalTED = rec.get("totalCost").asDouble();
      else
        totalTED = getTotalTED(nodes);

    } catch (NoSuchRecordException e) {
      // No path found - Create path to most similar correct node
      targetNode = db.getMostSimilarNode(sourceNode.get("ast").toString(),
              "Correct");
      if (targetNode != null) {
        // Create edge between the two nodes
        first_edge = db.addEdge(sourceNode, targetNode);
        nextNode = targetNode;
        totalTED = first_edge.get("ted").asDouble();
      } else {
        System.err.println("Error: Cannot generate hint.");
      }
    }
    // Generate hint message
    hint = new Hint(sourceNode, targetNode, first_edge);
    // Stop timer
    time = System.currentTimeMillis() - startTime;
  }

  // Getters

  /**
   * Returns the node from the database with the same AST. If it
   * does not exist, it searches for the most similar node,
   * creates a new node with the given expression and adds an edge
   * between these two nodes.
   *
   * @param ast AST of the expression
   * @return Node from the database with the given expression.
   */
  private Node getSourceNode(String ast) {
    Node source = db.getNodeByAST(ast);
    if (source == null) { // If it does not exist, create it
      isNewNode = true;
      // Get the most similar node
      Node similarNode = db.getMostSimilarNode(ast);
      if (similarNode != null) {
        // Create the node with the AST
        source = db.addIncorrectNode(expression, ast, code);
        // Add edge between the new node and the most similar node
        db.addEdge(source, similarNode);
      }
    } else {
      isNewNode = false;
    }
    return source;
  }

  /**
   * Calculates the total TED of the path to the solution.
   *
   * @param nodes List of nodes in the path to the solution.
   * @return Total TED of the path to the solution.
   */
  private double getTotalTED(List<Node> nodes) {
    double totalTED = 0;
    for (int i = 0; i < nodes.size() - 1; i++) {
      Node src = nodes.get(i);
      Node dst = nodes.get(i + 1);
      Relationship edge = db.getRelationship(src, dst);
      totalTED += edge.get("ted").asDouble();
    }
    return totalTED;
  }

  public JSONObject getJSON() {
    JSONObject json = new JSONObject();
    json.put("expression", expression);
    json.put("code", code);
    json.put("type", type.toString());
    json.put("isNewNode", isNewNode);
    json.put("sourceExpr", sourceNode.get("expr").asString());
    json.put("sourceAST", sourceNode.get("ast").asString());
    json.put("targetExpr", targetNode.get("expr").asString());
    json.put("targetAST", targetNode.get("ast").asString());
    json.put("nextExpr", nextNode.get("expr").asString());
    json.put("nextAST", nextNode.get("ast").asString());
    json.put("totalTED", totalTED);
    json.put("srcDstTED", hint.getDistance());
    json.put("Operations", first_edge.get("operations").toString());
    json.put("hint", hint.toHintMsg());
    json.put("time", time);
    return json;
  }

  public Hint getHint() {
    return hint;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    // Submission
    sb.append("Submission:\n")
            .append('\t').append(expression)
            // Source node
            .append("\nSource node:")
            .append("\n\tIs new node: ").append(isNewNode ? "Yes" : "No")
            .append("\n\tExpression: ").append(sourceNode.get("expr").asString())
            .append("\n\tAST: ").append(sourceNode.get("ast").asString());
    // Next node
    String nextId = nextNode.get("id").asString(), targetId = targetNode.get("id").asString();
    if (!nextId.equals(targetId)) {
      sb.append("\nNext node:")
              .append("\n\tExpression: ").append(nextNode.get("expr").asString())
              .append("\n\tAST: ").append(nextNode.get("ast").asString());
    }
    // Target node
    sb.append("\nTarget node:")
            .append("\n\tExpression: ").append(targetNode.get("expr").asString())
            .append("\n\tAST: ").append(targetNode.get("ast").asString())
            // First edge
            .append("\nPath:")
            .append("\n\tTotal TED: ").append(totalTED)
            .append("\n\tTED(source,target): ").append(hint.getDistance())
            .append("\n\tOperations: ").append(first_edge.get("operations").toString())
            // Time
            .append("\nTime:\n\t").append(time).append(" ms")
            // Hint
            .append("\nHint:\n\t").append(hint.toHintMsg());

    return sb.toString();
  }
}

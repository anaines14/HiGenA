package org.higena.graph.hint;

import edu.mit.csail.sdg.parser.CompModule;
import org.higena.A4FParser;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

public class HintGenerator {

  // Student submission
  private String expression, code;
  // Hint generation technique
  private HintGenType type;
  // Hint generation data
  private boolean isNewNode; // True if submission is new on the graph
  private Node sourceNode; // Node from the graph with the same AST as the submission
  private Node targetNode; // Closest solution in the graph
  private Node nextNode; // Next node in the path to the solution
  private Relationship first_edge; // First edge in the path to the solution
  private int totalCost; // Total cost of the path to the solution
  private Hint hint; // Generated hint

  public HintGenerator(String expression, String code, HintGenType type) {
    this.expression = expression;
    this.code = code;
    this.type = type;
  }

  public void generateHint() {
    // Get source node



  }

  // Parsing methods

  /**
   * Parses an Alloy expression using the challenge module and returns the AST
   * of the parsed expression.
   *
   * @param expression The expression to parse.
   * @return The AST of the parsed expression.
   */
  private String parseExpr(String expression, CompModule challenge_module) {
    if (expression.equals("")) {
      return "";
    }
    return A4FParser.parse(expression, challenge_module).toString();
  }

  /**
   * Parses an Alloy expression using the full module code and returns the AST of the parsed expression.
   *
   * @param expression The expression to parse.
   * @param code The full module code.
   * @return The AST of the parsed expression.
   */
  private String parseExpr(String expression, String code) {
    return A4FParser.parse(expression, code).toString();
  }




}

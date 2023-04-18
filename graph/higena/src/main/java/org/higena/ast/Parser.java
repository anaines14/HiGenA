package org.higena.ast;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import org.higena.A4FParser;

public class Parser {
  private final CompModule challenge_module;
  private final String file_extension = ".als";

  public Parser(String challenge) {
    // Path to the file with the challenge
    String challenges_path = "src/main/resources/challenges/",
            file = challenges_path + challenge + ".als";
    // Parse the file and store its module
    this.challenge_module = CompUtil.parseEverything_fromFile(new A4Reporter(), null, file);
  }

  // Parse functions

  /**
   * Parses an Alloy expression using the challenge module and returns the AST
   * of the parsed expression.
   *
   * @param expression The expression to parse.
   * @return The AST of the parsed expression.
   */
  private String parse(String expression) {
    if (expression.equals("")) {
      return "";
    }
    return A4FParser.parse(expression, this.challenge_module).toString();
  }

  /**
   * Parses an Alloy expression using the full module code and returns the AST of the parsed expression.
   *
   * @param expression The expression to parse.
   * @param code       The full module code.
   * @return The AST of the parsed expression.
   */
  public String parse(String expression, String code) {
    String ast;
    try {
      ast = parse(expression);
    } catch (Exception parseExprException) {
      try {
        ast = A4FParser.parse(expression, code).toString();
      } catch (Exception parseException) {
        System.err.println("Error parsing expression: " + expression);
        return null;
      }
    }
    return ast;
  }
}

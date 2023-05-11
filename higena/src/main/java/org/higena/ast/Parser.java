package org.higena.ast;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import org.parser.A4FParser;

public class Parser {
  private final CompModule challengeModule;

  public Parser(CompModule challengeModule) {
    this.challengeModule = challengeModule;
  }

  public static Parser fromFile(String file) {
    return new Parser(CompUtil.parseEverything_fromFile(new A4Reporter(), null, file));
  }

  public static Parser fromModel(String model) {
    return new Parser(CompUtil.parseEverything_fromString(new A4Reporter(), model));
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
    return A4FParser.parse(expression, this.challengeModule).toString();
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

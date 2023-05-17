package org.higena.parser;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the text between curly braces of an Alloy predicate.
 */
public class ExprExtractor {

  private final String code;
  private final Stack<Character> stack;

  public ExprExtractor(String code) {
    this.code = code;
    stack = new Stack<>();
  }

  public String parse(String predicateName) {
    StringBuilder expr = new StringBuilder();
    boolean inExpr = false;
    // Find the predicate
    int exprIndex =
            findWordIndex(predicateName, code) + predicateName.length();

    if (exprIndex == -1) {
      System.err.println("ERROR: Predicate not found.");
      return null;
    }

    // Parse the predicate
    for (int i = exprIndex; i < code.length(); i++) {
      char c = code.charAt(i);
      if (c == '}') {
        stack.pop();
        if (stack.isEmpty()) {
          return expr.toString();
        }
      }
      if (inExpr) {
        expr.append(c);
      }
      if (c == '{') {
        inExpr = true;
        stack.push(c);
      }
    }
    return expr.toString();
  }

  private int findWordIndex(String word, String line) {
    String regex = word + "\\s*\\{";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(line);

    if (matcher.find()) {
      return matcher.start();
    }

    return -1;
  }
}

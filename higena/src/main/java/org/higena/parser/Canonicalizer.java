package org.higena.parser;

import com.github.gumtreediff.tree.Tree;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Canonicalizes an Alloy AST.
 */
public class Canonicalizer {
  static List<String> commutativeOps = Arrays.asList("AND", "OR", "&&", "||", "&",
          "=", "!=", "<=>", "iff", "+");

  public static Tree canonicalize(Tree tree) {
    List<Tree> children = tree.getChildren();

    // Canonicalize children
    for (Tree child : children) {
      canonicalize(child);
    }

    // Commutative operation
    if (isCommutative(tree.getLabel())) {
      // Sort children
      children.sort(Comparator.comparing(Tree::toString));
    }

    return tree;
  }

   public static boolean isCommutative(String op) {
    return commutativeOps.contains(op.toUpperCase());
  }
}

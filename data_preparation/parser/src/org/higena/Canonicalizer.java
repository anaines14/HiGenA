package org.higena;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Canonicalizer {
  static List<String> commutativeOps = Arrays.asList("AND", "OR", "&&", "||", "&",
          "=", "!=", "<=>", "iff", "+");

  public static A4FNode canonicalize(A4FNode tree) {
    List<A4FNode> children = tree.getChildren();

    // Canonicalize children
    for (A4FNode child : children) {
      canonicalize(child);
    }

    // Commutative operation
    if (isCommutative(tree.getName())) {
      // Sort children
      children.sort(Comparator.comparing(A4FNode::toString));
    }

    return tree;
  }

  public static boolean isCommutative(String op) {
    return commutativeOps.contains(op.toUpperCase());
  }
}

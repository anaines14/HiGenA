package org.example.ast;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;

import java.util.*;

/**
 * Traverses the AST (post-order) and stores the index of each node.
 * Traverses the root, then the rightmost subtree, then the left subtree.
 * It checks the right first, so it is equal to APTED's post-order traversal.
 */
public class TraversedAST {
  private final Node<StringNodeData> node;
  private final List<TraversedAST> children;
  private final int index;

  public TraversedAST(Node<StringNodeData> root) {
    this(root, 1);
  }

  public TraversedAST(Node<StringNodeData> node, int index) {
    this.node = node;
    this.index = index;
    this.children = new ArrayList<>();

    int childIndex = index + 1;

    for (int i = node.getChildren().size(); i-- > 0; ) {
      Node<StringNodeData> child = node.getChildren().get(i);
      this.children.add(new TraversedAST(child, childIndex));
      childIndex += child.getNodeCount();
    }
  }

  @Override
  public String toString() {
    StringBuilder ret =
            new StringBuilder(node.getNodeData().getLabel() + " " + index + "\n");
    for (TraversedAST child : children) {
      ret.append(child.toString());
    }
    return ret.toString();
  }

  public Map<Integer, String> getIndexNodes() {
    Map<Integer, String> ret = new HashMap<>();
    ret.put(index, node.getNodeData().getLabel());
    for (TraversedAST child : children) {
      ret.putAll(child.getIndexNodes());
    }
    return ret;
  }
}

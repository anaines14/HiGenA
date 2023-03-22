package org.higena.ast.actions;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;
import com.github.gumtreediff.tree.Tree;

import java.util.ArrayList;
import java.util.List;

public class ActionNode {
  private final List<ActionNode> children;
  private final String label;

  public ActionNode(Tree tree) {
    this.children = new ArrayList<>();
    this.label = tree.getLabel();

    for (Tree child : tree.getChildren()) {
      this.children.add(new ActionNode(child));
    }
  }

  public ActionNode (Node<StringNodeData> node) {
    this.children = new ArrayList<>();
    this.label = node.getNodeData().getLabel();

    for (Node<StringNodeData> child : node.getChildren()) {
      this.children.add(new ActionNode(child));
    }
  }

  public ActionNode (String treeStr) {
    this(new BracketStringInputParser().fromString(treeStr));
  }

  public ActionNode (String label, List<ActionNode> children) {
    this.children = children;
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder("{" + label);
    for (ActionNode child : children) {
      ret.append(child.toString());
    }
    ret.append("}");
    return ret.toString();
  }
}

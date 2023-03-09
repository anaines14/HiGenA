package org.example.ast.actions;

import com.github.gumtreediff.tree.Tree;

import java.util.ArrayList;
import java.util.List;

public class ActionNode {
  private List<ActionNode> children;
  private String label;

  public ActionNode(Tree tree) {
    this.label = tree.getLabel();
    this.children = new ArrayList<>();

    for (Tree child : tree.getChildren()) {
      this.children.add(new ActionNode(child));
    }
  }

    public ActionNode() {
        this.label = "";
        this.children = new ArrayList<>();
    }

  public List<ActionNode> getChildren() {
    return children;
  }

  public String getLabel() {
    return label;
  }

  public void setChildren(List<ActionNode> children) {
    this.children = children;
  }

  public void setLabel(String label) {
    this.label = label;
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

package org.higena.ast.actions;

import com.github.gumtreediff.tree.Tree;

import java.util.ArrayList;
import java.util.List;

public class ActionNode {
  private final List<ActionNode> children;
  private final String label;

  public ActionNode(Tree tree) {
    this.children = new ArrayList<>();
    if (tree != null) {
      this.label = tree.getLabel();

      for (Tree child : tree.getChildren()) {
        this.children.add(new ActionNode(child));
      }
    } else {
      this.label = "root";
    }
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

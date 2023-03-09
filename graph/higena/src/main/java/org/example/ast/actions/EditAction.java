package org.example.ast.actions;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;
import org.example.ast.AST;

public class EditAction {
  private final String type;
  private final AST node;
  private AST parent;
  private int position;
  private String value;

  public EditAction(Action action) {
    this.type = action.getClass().getSimpleName();
    this.node = new AST(action.getNode());

    if (action instanceof TreeAddition treeAddition) {
      this.parent = (treeAddition.getParent() == null) ? null : new AST(treeAddition.getParent());
      this.position = treeAddition.getPosition();
    }
    else if (action instanceof Addition addition) {
      this.parent = (addition.getParent() == null) ? null : new AST(addition.getParent());
      this.position = addition.getPosition();
    }
    else if (action instanceof Update) {
      this.value = ((Update) action).getValue();
    }
  }

  @Override
  public String toString() {
    String ret = "{" + "type='" + type + '\'' +  ", node=";

    switch (type) {
      case "TreeAddition" -> {
        ret += node.toTreeString() + ", parent=";
        ret += (parent != null) ? parent.toString() : "root";
        ret += ", position=" + position + '}';
      }
      case "Addition" -> {
        ret += node.toString() + ", parent=";
        ret += (parent != null) ? parent.toString() : "root";
        ret += ", position=" + position + '}';
      }
      case "Update" -> ret += node.toString() + ", value=" + value + '}';
      default -> ret += node.toString() + '}';
    }


    return ret;
  }
}

package org.higena.ast.actions;

import com.github.gumtreediff.actions.model.*;

public class EditAction {
  private final String type;
  private final ActionNode node;
  private ActionNode parent;
  private int position;
  private String value;

  public EditAction(Action action) {
    this.type = action.getClass().getSimpleName();
    this.node = new ActionNode(action.getNode());

    if (action instanceof TreeAddition treeAddition) {
      this.parent = new ActionNode(treeAddition.getParent());
      this.position = treeAddition.getPosition();
    }
    else if (action instanceof Addition addition) {
      this.parent = new ActionNode(addition.getParent());
      this.position = addition.getPosition();
    }
    else if (action instanceof Update) {
      this.value = ((Update) action).getValue();
    }
  }

  @Override
  public String toString() {
    String ret = "\"{" + "type='" + type + '\'';

    switch (type) {
      case "TreeAddition", "Move", "TreeInsert" -> {
        ret += ", tree=" + node.toString() + ", parent=";
        ret += parent.getLabel();
        ret += ", position=" + position + "}\"";
      }
      case "Addition", "Insert" -> {
        ret += ", node=" + node.getLabel() + ", parent=";
        ret += parent.getLabel();
        ret += ", position=" + position + "}\"";
      }
      case "Update" -> ret += ", node=" + node.getLabel() + ", value=" + value + "}\"";
      case "TreeDelete" -> ret += ", tree=" + node.toString() + "}\"";
      default -> ret += ", node=" + node.getLabel() + "}\"";
    }


    return ret;
  }
}

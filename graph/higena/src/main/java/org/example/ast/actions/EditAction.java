package org.example.ast.actions;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;

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

  public EditAction() {
    this.type = "";
    this.node = null;
    this.position = -1;
    this.value = "";
  }

  public String getType() {
    return this.type;
  }

  public ActionNode getNode() {
    return node;
  }

  public ActionNode getParent() {
    return parent;
  }

  public int getPosition() {
    return position;
  }

  public String getValue() {
    return value;
  }
}

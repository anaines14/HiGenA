package org.example.ast.actions;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;

public class EditAction {
  private final String type;
  private final Tree node;
  private Tree parent;
  private int position;
  private String value;

  public EditAction(Action action) {
    this.type = action.getClass().getSimpleName();
    this.node = action.getNode();

    if (action instanceof Addition) {
      setProperties((Addition) action);
    }
    else if (action instanceof TreeAddition) {
      setProperties((TreeAddition) action);
    }
    else if (action instanceof Update) {
      setProperties((Update) action);
    }
  }

  private void setProperties(Update action) {
    this.value = action.getValue();
  }

  public void setProperties(TreeAddition action) {
    this.parent = action.getParent();
    this.position = action.getPosition();
  }

  public void setProperties(Addition action) {
    this.parent = action.getParent();
    this.position = action.getPosition();
  }

  public String getType() {
    return this.type;
  }

  public Tree getNode() {
    return node;
  }

  public Tree getParent() {
    return parent;
  }

  public int getPosition() {
    return position;
  }

  public String getValue() {
    return value;
  }
}

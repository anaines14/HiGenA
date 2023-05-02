package org.higena.ast.actions;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the differences between two trees,
 * namely the TED (tree edit distance) and the edit actions that
 * transform one tree into the other.
 *
 * @see EditAction
 */
public class TreeDiff {
  private final int ted; // tree edit distance (TED)
  private final List<EditAction> actions; // edit actions that transform one tree into the other

  public TreeDiff(int ted) {
    this.ted = ted;
    this.actions = new ArrayList<>();
  }

  // Add actions methods

  public void addAction(EditAction action) {
    this.actions.add(action);
  }

  public void addAllActions(List<String> actions) {
    for (String action : actions) {
      this.actions.add(EditAction.fromString(action));
    }
  }

  // Getters

  public int getTed() {
    return ted;
  }

  public List<EditAction> getActions() {
    return actions;
  }
}

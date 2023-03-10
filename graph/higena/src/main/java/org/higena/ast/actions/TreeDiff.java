package org.higena.ast.actions;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the differences between two trees,
 * namely the TED (tree edit distance) and the edit actions that
 * transform one tree into the other.
 */
public class TreeDiff {
  private final int ted;
  private final List<EditAction> actions;

    public TreeDiff(int ted) {
        this.ted = ted;
        this.actions = new ArrayList<>();
    }

    public void addAction(EditAction action) {
        this.actions.add(action);
    }

  public int getTed() {
    return ted;
  }

  public List<EditAction> getActions() {
    return actions;
  }
}

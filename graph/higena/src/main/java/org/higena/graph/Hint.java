package org.higena.graph;

import org.higena.ast.actions.EditAction;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Hint {
  private final int distance;
  private final List<EditAction> actions;

  public Hint(int distance, Relationship rel) {
    this.distance = distance;
    this.actions = new ArrayList<>();
    rel.get("operations").asList(Value::asString).forEach(op -> actions.add(EditAction.fromString(op)));
  }

  public String toHintMsg() {
    return distanceToHint() + "\n" + actionsToHint();
  }

  private String actionsToHint() {
    int random = new Random().nextInt(actions.size()) % actions.size();
    return actionToHint(actions.get(random));
  }

  private String actionToHint(EditAction action) {
    String type = action.getType();
    String node = action.getNode().getLabel().replace("this/", ""),
            parent = action.getParent() != null ? action.getParent().getLabel().replace("this/", "") : null,
            value = action.getValue() != null ? action.getValue().replace("this/", "") : null;

    switch (type) {
      case "Update" -> {
        return "Try changing " + node + " to " + value + ". ";
      }
      case "Move" -> {
        return "Try changing the position of the " + node + " to " + parent + ". ";
      }
      case "TreeAddition", "TreeInsert", "Addition", "Insert" -> {
        return "Try adding " + node + " to " + parent + ". ";
      }
      case "TreeDelete", "Delete" -> {
        return "Try deleting " + node + ". ";
      }
      default -> {
        return "TODO hint for " + type + ". ";
      }
    }
  }

  private String distanceToHint() {
    if (distance == 0) {
      return "Good job! If you want you can try another approach.";
    } else if (distance == 1) {
      return "One step away from the solution!";
    } else if (distance <= 3) {
      return "Almost there!";
    } else if (distance <= 5) {
      return "Keep going! You are getting closer.";
    } else {
      return "Keep going! Try again!";
    }
  }

  public List<EditAction> getActions() {
    return actions;
  }
}

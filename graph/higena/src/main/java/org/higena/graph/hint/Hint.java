package org.higena.graph.hint;

import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.EditActionsComparator;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hint {
  private final double distance;
  private final List<EditAction> actions;

  public Hint(double distance, Relationship rel) {
    this.distance = distance;
    this.actions = new ArrayList<>();
    rel.get("operations").asList(Value::asString).forEach(op -> actions.add(EditAction.fromString(op)));
  }

  public static String actionToHint(EditAction action) {
    String type = action.getType();
    String node = action.getNode().getLabel().replace("this/", ""),
            parent = action.getParent() != null ? action.getParent().getLabel().replace("this/", "") : null,
            value = action.getValue() != null ? action.getValue().replace("this/", "") : null;

    switch (type) {
      case "Update" -> {
        return "Try changing \"" + node + "\" to \"" + value + "\". ";
      }
      case "Move" -> {
        if (parent != null) {
          String str = '"' + node + '"' + " is not in the right place.";
          if (!parent.equals("root"))
            return str + "Try moving it to the inside of " + parent + "\".";
          return str;
        }
      }
      case "TreeAddition", "TreeInsert", "Addition", "Insert" -> {
        if (parent != null) {
          String str = "Missing \"" + node + "\".";
          if (!parent.equals("root"))
            return str + "Try adding it inside of \"" + parent + "\".";
          return str;
        }
      }
      case "TreeDelete", "Delete" -> {
        return "Try deleting \"" + node + "\". ";
      }
      default -> {
        return "TODO hint for " + type + ". ";
      }
    }

    System.err.println("Failed to generate hint for " + action);
    return "";
  }

  public String toHintMsg() {
    return distanceToHint() + "\n" + actionsToHint();
  }

  private String actionsToHint() {
    actions.sort(new EditActionsComparator());
    return actionToHint(actions.get(0));
  }

  private String distanceToHint() {
    if (distance == 0) {
      return "Good job! If you want you can try another approach.";
    } else if (distance == 1) {
      return "One step away from the solution!";
    } else if (distance <= 3) {
      return "Near a solution!";
    } else {
      return "Keep going!";
    }
  }

  public List<EditAction> getActions() {
    return actions;
  }
}
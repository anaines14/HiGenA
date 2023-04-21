package org.higena.graph.hint;

import org.higena.ast.TED;
import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.EditActionsComparator;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Hint {
  private final double distance;
  private final List<EditAction> actions;

  public Hint(Node sourceNode, Node targetNode, Relationship rel) {
    this.distance = new TED().computeEditDistance(sourceNode.get("ast").asString(), targetNode.get("ast").asString());
    this.actions = new ArrayList<>();
    rel.get("operations").asList(Value::asString).forEach(op -> actions.add(EditAction.fromString(op)));
  }

  public static String actionToHint(EditAction action) {
    String type = action.getType();
    String node = replaceVariable(action.getNode().getLabel()),
            parent = replaceVariable(action.getParent() != null ?
                    action.getParent().getLabel() : null),
            value = replaceVariable(action.getValue() != null ?
                    action.getValue() : null);


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

  private static String replaceVariable(String value) {
    if (value == null) return null;
    // Match var0/Int, var1/Bool, etc.
    Pattern p = Pattern.compile("var\\d+/(\\w+)");
    Matcher m = p.matcher(value);
    if (m.matches()) {
      return "a variable of type " + m.group(1);
    }
    return value;
  }

  public String toHintMsg() {
    return distanceToHint() + " " + actionsToHint();
  }

  private String actionsToHint() {
    // Sort actions by priority
    actions.sort(new EditActionsComparator());
    // Return the first action
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

  // Getters

  public Double getDistance() {
    return distance;
  }
}

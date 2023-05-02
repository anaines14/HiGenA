package org.higena.graph.hint;

import org.higena.ast.TED;
import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.EditActionsComparator;
import org.higena.ast.actions.TreeDiff;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains the difference between two ASTs and uses this
 * information to generate a hint in text form.
 */
public class Hint {
  private final TreeDiff diff; // Difference between the two ASTs

  public Hint(Node sourceNode, Node targetNode, Relationship rel) {
    // Compute TED between source and target nodes
    this(TED.computeEditDistance(sourceNode.get("ast").asString(), targetNode.get("ast").asString()), rel);
  }

  public Hint(int distance, Relationship rel) {
    this.diff = new TreeDiff(distance);
    this.diff.addAllActions(rel.get("operations").asList(Value::asString));
  }

  /**
   * Uses the information of an edit action to generate part of a hint. Depending
   * on the type of the action, different hints are generated.
   *
   * @param action Edit action containing information to generate the hint
   * @return Part of a hint string.
   */
  public static String actionToHint(EditAction action) {
    String type = action.getType();
    String node = replaceVariable(action.getNode().getLabel()), parent = replaceVariable(action.getParent() != null ? action.getParent().getLabel() : null), value = replaceVariable(action.getValue() != null ? action.getValue() : null);


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

  /**
   * Replaces the variable name in a string with a more descriptive name. For
   * example, "var0/Int" is replaced with "a variable of type Int".
   *
   * @param value String containing a variable name
   * @return String with the variable name replaced
   */
  private static String replaceVariable(String value) {
    if (value == null) return null;
    // Match variable name (e.g.:var0/Int, var1/Bool, etc)
    Pattern p = Pattern.compile("var\\d+/(\\w+)");
    Matcher m = p.matcher(value);
    if (m.matches()) {
      return "a variable of type " + m.group(1);
    }
    return value;
  }

  /**
   * Generates part of a hint using the information of the edit actions. Given
   * a list of edit actions, the list is sorted by priority and the first action
   * is used to generate the hint.
   *
   * @return Hint string using edit actions
   */
  private String actionsToHint() {
    List<EditAction> actions = new ArrayList<>(this.diff.getActions());

    // Sort actions by priority
    actions.sort(new EditActionsComparator());

    // Return the first action in the form of a hint
    return actionToHint(actions.get(0));
  }

  /**
   * Generates part of a hint using the information of the TED. Depending on the
   * distance, different hints are generated.
   *
   * @return Hint string using TED
   */
  private String distanceToHint() {
    int distance = this.diff.getTed();
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

  /**
   * Generates a hint in text form using the information of the difference
   * between the two ASTs (TED and edit actions).
   * Example: Near a solution! Try changing "File" to "this/Trash".
   *
   * @return Hint in text form
   */
  @Override
  public String toString() {
    return distanceToHint() + " " + actionsToHint();
  }

  // Getters

  public int getDistance() {
    return this.diff.getTed();
  }
}

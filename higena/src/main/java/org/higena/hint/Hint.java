package org.higena.hint;

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
import java.util.stream.Collectors;

/**
 * This class contains the difference between two ASTs and uses this
 * information to generate a hint in text form.
 */
public class Hint {
  private final TreeDiff diff; // Difference between the two ASTs

  public Hint(Node sourceNode, Node targetNode, Relationship rel) {
    // Compute TED between source and target nodes
    this(TED.computeEditDistance(sourceNode.get("ast").asString(),
            targetNode.get("ast").asString()), rel);
  }

  public Hint(int distance, Relationship rel) {
    this.diff = new TreeDiff(distance);
    this.diff.addAllActions(rel.get("operations").asList(Value::asString));
  }

  /**
   * Uses the information of an edit action to generate part of a hint.
   * Depending
   * on the type of the action, different hints are generated.
   *
   * @param action Edit action containing information to generate the hint
   * @return Part of a hint string.
   */
  private static String actionToHint(EditAction action) {
    String type = action.getType();
    String node = action.getNode().getLabel(), parent =
            action.getParent() != null ? action.getParent().getLabel() : null
            , value = action.getValue() != null ? action.getValue() : null;

    switch (type) {
      case "Update":
        return updateToHint(node, value);

      case "Move":
        if (parent != null) {
          return moveToHint(node, parent);
        }
        break;

      case "TreeAddition":
      case "TreeInsert":
        // If the parent is "all" or "some" it is the addition of a variable
        // so we need to get the signature type
        if (parent != null) {
          if (parent.equals("all") || parent.equals("some")) {
            String regex = "sig/(\\w+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(action.getNode().toTreeString());
            if (matcher.find()) {
              String sig = matcher.group(1);
              return treeInsertToHint(sig, parent);
            }
          }
        }
      case "Addition":
      case "Insert":
        if (parent != null) {
          return insertToHint(node, parent);
        }
        break;

      case "TreeDelete":
      case "Delete":
        return deleteToHint(node);
    }

    System.err.println("Failed to generate hint for " + action);
    return "";
  }

  private static String updateToHint(String oldValue, String newValue) {
    List<String> oldDescription = getAlloyDescription(oldValue),
            newDescription =
                    getAlloyDescription(newValue);
    String oldName = oldDescription.get(0), newName =
            getAlloyDescription(newValue).get(0);
    String oldRole = oldDescription.get(1), newRole = newDescription.get(1);

    oldRole = oldRole.equals("") ? "" : oldRole;
    newRole = newRole.equals("") ? " to help satisfy the required property" :
            newRole;
    return "Instead of using " + oldName + oldRole
            + ", try using " + newName + newRole + ".";
  }

  private static String moveToHint(String node, String parent) {
    List<String> alloyDescription = getAlloyDescription(node);
    String nodeName = alloyDescription.get(0), parentName =
            getAlloyDescription(parent).get(0);

    String str =
            "It seems like the " + nodeName + "is not in the right place. ";
    if (!parent.equals("root"))
      str += "Try moving it to the inside of the " + parentName + "expression" +
              ". ";
    return str + " Try moving it so that you correctly ensure the required " + "property.";
  }

  private static String treeInsertToHint(String value, String parent) {
    // Missing variables
    if (parent.equals("all") || parent.equals("some"))
      return "You can use variables to help specify the condition. Consider " +
              "introducing a new variable \"" + value + "\" to your " +
              "expression using the " + getAlloyDescription(parent).get(0) +
              ".";

    return insertToHint(value, parent);
  }

  private static String insertToHint(String value, String parent) {
    List<String> alloyDescription = getAlloyDescription(value);
    String ret, name = alloyDescription.get(0), role = alloyDescription.get(1);

    if (value.equals("all") || value.equals("some")) {
      return "You can use variables to help specify the condition. Consider " +
              "introducing a new variable to your expression using the " + name + ".";
    } else if (name.contains("variable")) {
      ret = "You can use variables to help specify the condition. Consider " +
              "using a " + name + " to correctly capture the property you " +
              "want to specify.";
    } else {
      role = role.equals("") ? " to help satisfy the required property" : role;
      ret = "Consider adding a " + name + role + ".";
    }

    // Parent to hint
    if (parent.equals("root"))
      return ret + " Think about how you can incorporate this within your " +
              "expression to ensure the required property.";
    else
      return ret + " Think about how you can incorporate this within the " + getAlloyDescription(parent).get(0) + " expression.";
  }

  private static String deleteToHint(String node) {
    String name = getAlloyDescription(node).get(0);

    return "It seems like you have unnecessary elements in " +
            "your expression. You can try simplifying your expression by " +
            "deleting the " + name + ". If you want to keep it, try "
            + "to fix your " + "expression another way and reach a different " +
            "solution!";
  }

  private static List<String> getAlloyDescription(String label) {
    List<String> ret = new ArrayList<>();

    // Match variable name (e.g.:var0/Int, var1/Bool, etc)
    Pattern p = Pattern.compile("var\\d+/(\\w+)");
    Matcher m = p.matcher(label);
    if (m.matches()) {
      ret.add(0, "variable of type " + m.group(1));
      ret.add(1, "");
      return ret;
    }

    // Match signature name (e.g.: sig/Int, sig/Bool, etc)
    p = Pattern.compile("sig/(\\w+)");
    m = p.matcher(label);
    if (m.matches()) {
      ret.add(0, "signature of type " + m.group(1));
      ret.add(1, "");
      return ret;
    }

    switch (label.toLowerCase()) {
      // Set operators
      case ".":
        ret.add(0, "dot join operator ('.')");
        ret.add(1, " to perform a relational join " + "between sets or " +
                "relations");
        return ret;
      case "+":
        ret.add(0, "union operator ('+')");
        ret.add(1, " to combine two sets");
        return ret;
      case "&":
        ret.add(0, "intersection operator ('&')");
        ret.add(1, " to find the common elements between two sets");
        return ret;
      case "++":
        ret.add(0, "relational override operator ('++')");
        ret.add(1, " to combine two sets eliminating duplicates");
        return ret;
      case "-":
        ret.add(0, "difference operator ('-')");
        ret.add(1, " to remove elements from a set");
        return ret;
      case "in":
        ret.add(0, "inclusion operator ('in')");
        ret.add(1, " to specify that some element(s) belong to a set");
        return ret;
      case "not in":
      case "!in":
        ret.add(0, "exclusion operator ('!in')");
        ret.add(1, " to specify that some element(s) do not belong to a set");
        return ret;
      case "<:":
        ret.add(0, "restriction operator ('<:')");
        ret.add(1, " to restrict the domain of a relation");
        return ret;
      case ":>":
        ret.add(0, "restriction operator (':>')");
        ret.add(1, " to restrict the range of a relation");
        return ret;

      // Other operators
      case "not":
        ret.add(0, "negation operator ('not')");
        ret.add(1, " to specify that the expression is false");
        return ret;
      case "!":
        ret.add(0, "negation operator ('!')");
        ret.add(1, " to specify that the expression is false");
        return ret;
      case "~":
        ret.add(0, "transpose operator ('~')");
        ret.add(1, " to transpose a relation");
        return ret;
      case "^":
        ret.add(0, "transitive closure operator ('^')");
        ret.add(1, " to get the transitive closure of a relation");
        return ret;
      case "*":
        ret.add(0, "reflexive-transitive closure operator ('*')");
        ret.add(1, " to get the reflexive-transitive closure of a relation");
        return ret;
      case "implies":
      case "=>":
        ret.add(0, "implication operator ('=>')");
        ret.add(1,
                " to specify that if the left side is true, then the right " +
                        "side must also be true");
        return ret;
      case "iff":
        ret.add(0, "equivalence operator ('iff')");
        ret.add(1,
                " to specify the equivalence of the right and left side of " +
                        "the " +
                        "expression");
        return ret;
      case "<=>":
        ret.add(0, "equivalence operator ('<=>')");
        ret.add(1,
                " to specify the equivalence of the right and left side of " +
                        "the " +
                        "expression");
        return ret;
      case ">=":
        ret.add(0, "greater than or equal to operator ('>=')");
        ret.add(1,
                " to specify that the left side is greater than or equal to " +
                        "the right side");
        return ret;
      case "<":
        ret.add(0, "less than operator ('<')");
        ret.add(1, " to specify that the left side is less than the right " +
                "side");
        return ret;
      case ">":
        ret.add(0, "greater than operator ('>')");
        ret.add(1, " to specify that the left side is greater than the right " +
                "side");
        return ret;
      case "=<":
        ret.add(0, "less than or equal to operator ('=<')");
        ret.add(1, " to specify that the left side is less than or equal to " +
                "the right side");
        return ret;
      case "!=":
        ret.add(0, "not equal operator ('!=')");
        ret.add(1, " to specify that the left side is not equal to the right " +
                "side");
        return ret;
      case "=":
        ret.add(0, "equal operator ('=')");
        ret.add(1, " to specify that the left side is equal to the right " +
                "side");
        return ret;
      case "->":
        ret.add(0, "arrow operator ('->')");
        ret.add(1, " to map a relation");
        return ret;

      // Quantifiers
      case "one":
        ret.add(0, "unique quantifier ('one')");
        ret.add(1, " to specify that there is exactly one element in a set");
        return ret;
      case "no":
        ret.add(0, "no quantifier ('no')");
        ret.add(1, " to specify that there are no elements in a set");
        return ret;
      case "univ":
        ret.add(0, "universal quantifier ('univ')");
        ret.add(1, " to specify that all elements in a set satisfy a " +
                "condition");
        return ret;
      case "all":
        ret.add(0, "universal quantifier ('all')");
        ret.add(1, " to specify that all elements in a set satisfy a " +
                "condition");
        return ret;
      case "some":
        ret.add(0, "existential quantifier ('some')");
        ret.add(1, " to specify that some elements in a set satisfy a " +
                "condition");
        return ret;
      case "let":
        ret.add(0, "\"let\" ('let var = expression1 | expression2')");
        ret.add(1, " to introduce a new variable");
        return ret;
      case "lone":
        ret.add(0, "lone quantifier ('lone')");
        ret.add(1, " to specify that there is at most one element in a set");
        return ret;
      case "sum":
        ret.add(0, "sum quantifier ('sum')");
        ret.add(1,
                " to specify that the sum of the elements in a set satisfy a " +
                        "condition");
        return ret;
      case "seq":
        ret.add(0, "sequence constructor ('seq')");
        ret.add(1, " to specify that the elements in a set are ordered");
        return ret;
      case "none":
        ret.add(0, "empty set constructor ('none')");
        ret.add(1, " to specify that a set is empty");
        return ret;
      case "iden":
        ret.add(0, "identity relation constructor ('iden')");
        ret.add(1, " to specifies the identity relation");
        return ret;
      case "disj":
        ret.add(0, "disjoint operator ('disj')");
        ret.add(1, " to specify that two sets are disjoint");
        return ret;

      // Logic operators
      case "||":
        ret.add(0, "disjunction operator ('||')");
        ret.add(1, " to combine two boolean expressions");
        return ret;
      case "or":
        ret.add(0, "disjunction operator ('or')");
        ret.add(1, " to combine two boolean expressions");
        return ret;
      case "&&":
        ret.add(0, "conjunction operator ('&&')");
        ret.add(1, " to combine two boolean expressions");
        return ret;
      case "and":
        ret.add(0, "conjunction operator ('and')");
        ret.add(1, " to combine two boolean expressions");
        return ret;

      // Future Temporal operators
      case "always":
        ret.add(0, "temporal operator ('always')");
        ret.add(1, " to specify that a property should always hold");
        return ret;
      case "eventually":
        ret.add(0, "temporal operator ('eventually')");
        ret.add(1, " to specify that a property will eventually hold in the " +
                "future");
        return ret;
      case "after":
        ret.add(0, "temporal operator ('after')");
        ret.add(1, " to specify that a property will hold in the next state");
        return ret;
      case "until":
        ret.add(0, "temporal operator ('until')");
        ret.add(1, " to specify that a property will hold until another " +
                "property "
                + "holds");
        return ret;

      // Past Temporal operators
      case "before":
        ret.add(0, "temporal operator ('before')");
        ret.add(1, " to specify that a property will hold in the previous " +
                "state");
        return ret;
      case "once":
        ret.add(0, "temporal operator ('once')");
        ret.add(1, " to specify that a property once held in the past");
        return ret;
      case "historically":
        ret.add(0, "temporal operator ('historically')");
        ret.add(1, " to specify that a property always held in the past");
        return ret;
      case "since":
        ret.add(0, "temporal operator ('since')");
        ret.add(1, " to specify that a property holds since another property");
        return ret;
    }
    ret.add(0, label);
    ret.add(1, "");
    return ret;
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
    // Filter out actions that are not relevant for hint generation
    List<EditAction> filtered = actions.stream()
            .filter(action -> !action.isBadAction())
            .collect(Collectors.toList());

    if (filtered.size() > 0) {
      actions = filtered;
    }

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
   * Example: Near a solution! Try changing "File" to "Trash".
   *
   * @return Hint in text form
   */
  @Override
  public String toString() {
    return distanceToHint() + " " + actionsToHint();
  }

  public int getDistance() {
    return this.diff.getTed();
  }
}

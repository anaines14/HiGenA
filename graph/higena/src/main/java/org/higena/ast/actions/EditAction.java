package org.higena.ast.actions;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.actions.model.Update;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    } else if (action instanceof Addition addition) {
      this.parent = new ActionNode(addition.getParent());
      this.position = addition.getPosition();
    } else if (action instanceof Update) {
      this.value = ((Update) action).getValue();
    }
  }

  public EditAction(String type, ActionNode node, ActionNode parent, int position) {
    this.type = type;
    this.node = node;
    this.parent = parent;
    this.position = position;
  }

  public EditAction(String type, ActionNode node, String value) {
    this.type = type;
    this.node = node;
    this.value = value;
  }

  public EditAction(String type, ActionNode node) {
    this.type = type;
    this.node = node;
  }

  public static EditAction fromString(String actionStr) {
    String type = getMatch("type", actionStr), value = getMatch("value", actionStr),
            tree = getMatch("tree", actionStr), node = getMatch("node", actionStr),
            parent = getMatch("parent", actionStr), position = getMatch("position", actionStr);

    if (type != null) {
      switch (type) {
        case "TreeAddition", "Move", "TreeInsert" -> {
          if (tree != null && parent != null && position != null) {
            return new EditAction(type, new ActionNode(tree), new ActionNode(parent, new ArrayList<>()), Integer.parseInt(position));
          }
        }
        case "Addition", "Insert" -> {
          if (node != null && parent != null && position != null) {
            return new EditAction(type, new ActionNode(node, new ArrayList<>()), new ActionNode(parent, new ArrayList<>()), Integer.parseInt(position));
          }
        }
        case "Update" -> {
          if (node != null && value != null) {
            return new EditAction(type, new ActionNode(node, new ArrayList<>()), value);
          }
        }
        case "TreeDelete" -> {
          if (tree != null) {
            return new EditAction(type, new ActionNode(tree));
          }
        }
        default -> {
          if (node != null) {
            return new EditAction(type, new ActionNode(node, new ArrayList<>()));
          }
        }
      }
    }
    System.err.println("Error parsing EditAction: " + actionStr);
    return null;
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

  // Auxiliar methods

  private static String getMatch(String field, String actionStr) {
    Matcher match = Pattern.compile(getFieldRegex(field)).matcher(actionStr);
    return match.find() ? match.group(1) : null;
  }

  private static String getFieldRegex(String field) {
    switch (field) {
      case "type" -> {
        return "type='(\\w+)'";
      }
      case "node" -> {
        return "node=([a-zA-Z0-9/]+)";
      }
      case "parent" -> {
        return "parent=([a-zA-Z0-9/]+)";
      }
      case "position" -> {
        return "position=(\\d+)";
      }
      case "value" -> {
        return "value=([a-zA-Z0-9/]+)";
      }
      case "tree" -> {
        return "tree=(\\{.*?}),";
      }
    }
    return null;
  }
}

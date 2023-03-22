package org.higena.ast.actions;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.actions.model.Update;

import java.util.ArrayList;
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
    String regex = "\"\\{type='(\\w+)'";
    Pattern pattern = Pattern.compile(regex);
    var matcher = pattern.matcher(actionStr);

    if (matcher.find()) {
      String type = matcher.group(1);
      switch (type) {
        case "TreeAddition", "Move", "TreeInsert" -> {
          regex = "tree=(\\{.*?\\}), parent=([a-zA-Z0-9/]+), position=(\\d+)\\}\"";
          pattern = Pattern.compile(regex);
          matcher = pattern.matcher(actionStr);

          if (matcher.find()) {
            var node = new ActionNode(matcher.group(1));
            var parent = new ActionNode(matcher.group(2), new ArrayList<>());
            var position = Integer.parseInt(matcher.group(3));

            return new EditAction(type, node, parent, position);
          }
        }
        case "Addition", "Insert" -> {
            regex = "node=([a-zA-Z0-9/]+), parent=([a-zA-Z0-9/]+), position=(\\d+)\\}\"";
            pattern = Pattern.compile(regex);
            matcher = pattern.matcher(actionStr);

            if (matcher.find()) {
                var node = new ActionNode(matcher.group(1), new ArrayList<>());
                var parent = new ActionNode(matcher.group(2), new ArrayList<>());
                var position = Integer.parseInt(matcher.group(3));

                return new EditAction(type, node, parent, position);
            }
        }
        case "Update" -> {
          regex = "node=([a-zA-Z0-9/]+), value=([a-zA-Z0-9/]+)\\}\"";
          pattern = Pattern.compile(regex);
          matcher = pattern.matcher(actionStr);

          if (matcher.find()) {
            var node = new ActionNode(matcher.group(1), new ArrayList<>());
            var value = matcher.group(2);

            return new EditAction(type, node, value);
          }
        }
        case "TreeDelete" -> {
            regex = "tree=(\\{.*?\\})\\}\"";
            pattern = Pattern.compile(regex);
            matcher = pattern.matcher(actionStr);

            if (matcher.find()) {
                var node = new ActionNode(matcher.group(1));
                return new EditAction(type, node);
            }
        }
        default -> {
          regex = "node=([a-zA-Z0-9/]+)\\}\"";
          pattern = Pattern.compile(regex);
          matcher = pattern.matcher(actionStr);

            if (matcher.find()) {
                var node = new ActionNode(matcher.group(1), new ArrayList<>());
                return new EditAction(type, node);
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
      case "Update" ->
              ret += ", node=" + node.getLabel() + ", value=" + value + "}\"";
      case "TreeDelete" -> ret += ", tree=" + node.toString() + "}\"";
      default -> ret += ", node=" + node.getLabel() + "}\"";
    }
    return ret;
  }
}

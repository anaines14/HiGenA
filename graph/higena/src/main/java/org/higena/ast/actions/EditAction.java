package org.higena.ast.actions;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;
import org.higena.ast.AlloyAST;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditAction {
  private final String type;
  private final Tree node;
  private Tree parent;
  private int position;
  private String value;

  public EditAction(Action action) {
    this.type = action.getClass().getSimpleName();
    this.node = action.getNode().deepCopy();

    if (action instanceof TreeAddition treeAddition) {
      this.parent = treeAddition.getParent().deepCopy();
      this.position = treeAddition.getPosition();
    } else if (action instanceof Addition addition) {
      this.parent = addition.getParent().deepCopy();
      this.position = addition.getPosition();
    } else if (action instanceof Update) {
      this.value = ((Update) action).getValue();
    }
  }

  public EditAction(String type, Tree node, Tree parent, int position) {
    this.type = type;
    this.node = node;
    this.parent = parent;
    this.position = position;
  }

  public EditAction(String type, Tree node, String value) {
    this.type = type;
    this.node = node;
    this.value = value;
  }

  public EditAction(String type, Tree node) {
    this.type = type;
    this.node = node;
  }

  public static EditAction fromString(String actionStr) {
    String type = getMatch("type", actionStr), value = getMatch("value", actionStr), tree = getMatch("tree", actionStr), node = getMatch("node", actionStr), parent = getMatch("parent", actionStr), position = getMatch("position", actionStr);
    if (type != null) {
      switch (type) {
        case "TreeAddition", "Move", "TreeInsert" -> {
          if (tree != null && parent != null && position != null) {
            Node<StringNodeData> parsedTree = new BracketStringInputParser().fromString(tree);
            return new EditAction(type, new AlloyAST(parsedTree), new AlloyAST(parent), Integer.parseInt(position));
          }
        }
        case "Addition", "Insert" -> {
          if (node != null && parent != null && position != null) {
            return new EditAction(type, new AlloyAST(node),
                    new AlloyAST(parent), Integer.parseInt(position));
          }
        }
        case "Update" -> {
          if (node != null && value != null) {
            return new EditAction(type, new AlloyAST(node), value);
          }
        }
        case "TreeDelete" -> {
          if (tree != null) {
            Node<StringNodeData> parsedTree = new BracketStringInputParser().fromString(tree);
            return new EditAction(type, new AlloyAST(parsedTree));
          }
        }
        default -> {
          if (node != null) {
            return new EditAction(type, new AlloyAST(node));
          }
        }
      }
    }
    System.err.println("Error parsing EditAction: " + actionStr);

    return null;
  }

  private static String getMatch(String field, String actionStr) {
    String regex = getFieldRegex(field);
    if (regex == null) return null;
    Matcher match = Pattern.compile(regex).matcher(actionStr);
    return match.find() ? match.group(1) : null;
  }

  // Auxiliary methods

  private static String getFieldRegex(String field) {
    String nodesRegex = "(['#:=><*~^.!a-zA-Z0-9/&+-]+)";
    switch (field) {
      case "type" -> {
        return "type='(\\w+)'";
      }
      case "node" -> {
        return "node=" + nodesRegex;
      }
      case "parent" -> {
        return "parent=" + nodesRegex;
      }
      case "position" -> {
        return "position=(\\d+)";
      }
      case "value" -> {
        return "value=" + nodesRegex;
      }
      case "tree" -> {
        return "tree='(\\{.*?})'";
      }
    }
    return null;
  }

  @Override
  public String toString() {
    String ret = "\"(" + "type='" + type + '\'';

    switch (type) {
      case "TreeAddition", "Move", "TreeInsert" -> {
        ret += ", tree='" + node.toTreeString() + "', parent=";
        ret += parent.getLabel();
        ret += ", position=" + position;
      }
      case "Addition", "Insert" -> {
        ret += ", node=" + node.getLabel() + ", parent=";
        ret += parent.getLabel();
        ret += ", position=" + position;
      }
      case "Update" -> ret += ", node=" + node.getLabel() + ", value=" + value;
      case "TreeDelete" -> ret += ", tree='" + node.toTreeString() + "'";
      default -> ret += ", node=" + node.getLabel();
    }
    return ret + ")\"";
  }

  // Getters

  public String getType() {
    return type;
  }

  public Tree getNode() {
    return node;
  }

  public Tree getParent() {
    return parent;
  }

  public String getValue() {
    return value;
  }

}

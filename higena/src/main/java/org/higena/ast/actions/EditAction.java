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

/**
 * Represents a transformation action that can be applied to the AST. There
 * are several types of actions:
 * <ul>
 *   <li>TreeAddition/TreeInsert/Addition/Insert: inserts a tree as a child
 *   of a node at a
 *   specified position</li>
 *   <li>Mode: moves a tree into the children of a node at a specified
 *   position</li>
 *   <li>Update: updates the value of a node</li>
 *   <li>TreeDelete/Delete: deletes a tree</li>
 * </ul>
 */
public class EditAction {
  private final String type; // Type of the action
  private final Tree node; // Node that is affected by the action
  private Tree parent; // Parent of the node (only for Addition)
  private int position; // Position of the node in the parent (only for Addition)
  private String value; // Value of the node (only for Update)

  private static final BracketStringInputParser parser =
          new BracketStringInputParser(); // Parser for string representations of trees

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

  // String representation methods

  /**
   * Parses a string representation of an action into an EditAction object.
   * @param actionStr String representation of an action to parse.
   * @return EditAction object.
   */
  public static EditAction fromString(String actionStr) {
    String type = getMatch("type", actionStr), value = getMatch("value", actionStr), tree = getMatch("tree", actionStr), node = getMatch("node", actionStr), parent = getMatch("parent", actionStr), position = getMatch("position", actionStr);
    if (type != null) {
      switch (type) {
        case "TreeAddition", "Move", "TreeInsert" -> {
          if (tree != null && parent != null && position != null) {
            Node<StringNodeData> parsedTree = parser.fromString(tree);
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
            Node<StringNodeData> parsedTree = parser.fromString(tree);
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

  /**
   * Given a string representation of an EditAction, returns the value of
   * a specific field. For example, for the string (type='Update', node=no,
   * value=File), the string value of the field "type" is "Update".
   *
   * @param field Field to get the value of (e.g.: type, node, parent,
   *              position, value, tree).
   * @param actionStr String representation of an EditAction.
   * @return Value of the field in the string representation.
   */
  private static String getMatch(String field, String actionStr) {
    String regex = getFieldRegex(field);
    if (regex == null) return null;
    Matcher match = Pattern.compile(regex).matcher(actionStr);
    return match.find() ? match.group(1) : null;
  }

  /**
   * Returns the regex necessary to match an EditAction field in its string representation.
   * @param field Field to match (e.g.: type, node, parent, position, value,
   *              tree).
   * @return Regex that matches the field.
   */
  private static String getFieldRegex(String field) {
    // Regex to match a node label
    String nodesRegex = "(['#:=><*~^.!a-zA-Z0-9/&+-]+)";
    switch (field) {
      case "type" -> { // Example: type='TreeAddition'
        return "type='(\\w+)'";
      }
      case "node" -> { // Example: node=no
        return "node=" + nodesRegex;
      }
      case "parent" -> { // Example: parent=root
        return "parent=" + nodesRegex;
      }
      case "position" -> { // Example: position=0
        return "position=(\\d+)";
      }
      case "value" -> { // Example: value=File
        return "value=" + nodesRegex;
      }
      case "tree" -> { // Example: tree='{no{Protected}}'
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
        // Example: (type='TreeInsert', tree='{AND{no{Protected}}}', parent=root, position=0)
        ret += ", tree='" + node.toTreeString() + "', parent=";
        ret += parent.getLabel();
        ret += ", position=" + position;
      }
      case "Addition", "Insert" -> {
        // Example: (type='Insert', node=no, parent=root, position=0)
        ret += ", node=" + node.getLabel() + ", parent=";
        ret += parent.getLabel();
        ret += ", position=" + position;
      }
      // Example: (type='Update', node=no, value=File)
      case "Update" -> ret += ", node=" + node.getLabel() + ", value=" + value;
      // Example: (type='TreeDelete', tree='{AND{no{Protected}}}')
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

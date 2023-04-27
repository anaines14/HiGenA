package org.higena.ast;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.tree.AbstractTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import org.higena.Canonicalizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class AlloyAST extends AbstractTree {
  private String label;

  public AlloyAST(Node<StringNodeData> root) {
    this.children = new ArrayList<>();
    this.setLabel(root.getNodeData().getLabel());

    for (Node<StringNodeData> child : root.getChildren()) {
      this.addChild(new AlloyAST(child));
    }
  }

  public AlloyAST(Tree other) {
    this.children = new ArrayList<>();
    this.label = other.getLabel();
  }

  public AlloyAST(Tree t, Tree parent) {
    this.children = new ArrayList<>();
    this.label = t.getLabel();
    for (Tree child : t.getChildren()) {
      this.addChild(child.deepCopy());
    }
    setParent(parent);
  }

  /**
   * Returns true if the two trees are equal. Two trees are equal if they have the same label and
   * the same children.
   *
   * @param t1 The first tree
   * @param t2 The second tree
   * @return True if the two trees are equal
   */
  public static boolean areEqual(Tree t1, Tree t2) {
    // Null check
    if (t1 == null && t2 == null) {
      return false;
    }

    // Label check
    if (!t1.getLabel().equals(t2.getLabel())) {
      return false;
    }

    // Number of children check
    if (t1.getChildren().size() != t2.getChildren().size()) {
      return false;
    }

    // Children check
    List<Tree> children1 = t1.getChildren(),
            children2 = t2.getChildren();
    for (int i = 0; i < children1.size(); i++) {
      Tree child1 = children1.get(i),
              child2 = children2.get(i);
      if (!areEqual(child1, child2)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Tree deepCopy() {
    Tree copy = new AlloyAST(this);
    for (Tree child : this.getChildren()) {
      copy.addChild(child.deepCopy());
    }
    return copy;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public void setLabel(String s) {
    this.label = s;
  }

  @Override
  public int getPos() {
    return 0;
  }

  @Override
  public void setPos(int i) {

  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public void setLength(int i) {

  }

  @Override
  public Type getType() {
    return null;
  }

  @Override
  public void setType(Type type) {

  }

  @Override
  public String toTreeString() {
    StringBuilder ret = new StringBuilder("{" + label);
    for (Tree child : children) {
      ret.append(child.toTreeString());
    }
    ret.append("}");
    return ret.toString();
  }

  @Override
  public Object getMetadata(String s) {
    return null;
  }

  @Override
  public Object setMetadata(String s, Object o) {
    return null;
  }

  @Override
  public Iterator<Map.Entry<String, Object>> getMetadata() {
    return null;
  }

  @Override
  public String toString() {
    return label;
  }

  /**
   * If the two ASTs have the same commutative operation with one branch in
   * common but in different positions, it swaps the children of the current
   * AST.
   *
   * @param other The other AST.
   */
  public void prepareForMatching(AlloyAST other) {
    List<AlloyAST> commutative1 = findCommutativeNodes();
    List<AlloyAST> commutative2 = other.findCommutativeNodes();

    for (AlloyAST current : commutative1) {
      for (AlloyAST otherCurrent : commutative2) {
        if (current.label.equals(otherCurrent.label) && current.hasCommonSwappedBranchWith(otherCurrent)) {
          current.swapChildren();
          break;
        }
      }
    }
  }

  /**
   * Returns a list of commutative nodes in the AST.
   *
   * @return List of commutative nodes
   */
  private List<AlloyAST> findCommutativeNodes() {
    List<Tree> allChildren = new ArrayList<>();
    List<AlloyAST> commutative = new ArrayList<>();

    // Add itself if it is commutative
    if (isCommutative()) {
      commutative.add(this);
    }
    // Check children
    for (Tree child : children) {
      AlloyAST childAST = new AlloyAST(child, this);
      commutative.addAll(childAST.findCommutativeNodes());
      allChildren.add(childAST);
    }

    // Update children
    this.children = allChildren;

    return commutative;
  }

  /**
   * Returns true if the label of the root represents a commutative operation
   * in Alloy.
   *
   * @return True if the label is commutative
   */
  private boolean isCommutative() {
    return Canonicalizer.isCommutative(label);
  }

  /**
   * Return true if both ASTs have two branches in common in swapped
   * positions. Example: The left branch of this AST is equal to the right
   * branch of the other AST or vice versa.
   *
   * @param other The other AST to compare to.
   * @return True if the two ASTs have a common branch
   */
  private boolean hasCommonSwappedBranchWith(AlloyAST other) {
    // Null check
    if (other == null) {
      return false;
    }

    // Equal number of children
    if (other.children.size() != children.size()) {
      return false;
    }

    if (children.size() != 2) {
      return false;
    }

    // Check left equals other's right
    Tree left = children.get(0), right = children.get(1),
            otherLeft = other.children.get(0), otherRight = other.children.get(1);

    if (areEqual(left, otherRight)) {
      return true;
    }

    // Check right equals other's left
    return areEqual(right, otherLeft);
  }

  /**
   * Swaps the children of this AST if the AST has two children.
   */
  private void swapChildren() {
    if (children.size() != 2) {
      return;
    }
    Tree temp = children.get(0);
    children.set(0, children.get(1));
    children.set(1, temp);
  }

}

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
  private List<Tree> children = new ArrayList<>();

  public AlloyAST(Node<StringNodeData> root) {
    this.setLabel(root.getNodeData().getLabel());

    for (Node<StringNodeData> child : root.getChildren()) {
      this.addChild(new AlloyAST(child));
    }
  }

  public AlloyAST(Tree other) {
    this.label = other.getLabel();
  }

  public AlloyAST(Tree t, Tree parent) {
   this.label = t.getLabel();
    for (Tree child : t.getChildren()) {
      this.addChild(child.deepCopy());
    }
    setParent(parent);
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
   * Returns a list of commutative nodes in the AST..
   *
   * @return List of commutative nodes
   */
  private List<AlloyAST> findCommutativeNodes() {
    List<Tree> allchildren = new ArrayList<>();
    List<AlloyAST> commutative = new ArrayList<>();

    // Add itself if it is commutative
    if (isCommutative()) {
      commutative.add(this);
    }
    // Check children
    for (Tree child : children) {
      AlloyAST childAST = new AlloyAST(child, this);
      commutative.addAll(childAST.findCommutativeNodes());
      allchildren.add(childAST);
    }

    // Update children
    this.children = allchildren;

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
    Tree left =  children.get(0), right = children.get(1),
            otherLeft = other.children.get(0), otherRight = other.children.get(1);

    if (left.equals(otherRight)) {
      return true;
    }

    // Check right equals other's left
    return right.equals(otherLeft);
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

  // To string methods

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
  public String toString() {
    return label;
  }

  // Getters

  @Override
  public Tree getParent() {
    return parent;
  }

  @Override
  public List<Tree> getChildren() {
    return children;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public int getPos() {
    return 0;
  }

  @Override
  public Type getType() {
    return null;
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public Iterator<Map.Entry<String, Object>> getMetadata() {
    return null;
  }

  @Override
  public Object getMetadata(String s) {
    return null;
  }

  // Setters

  @Override
  public void setParent(Tree tree) {
    this.parent = tree;
  }

  @Override
  public void setChildren(List<Tree> list) {
    this.children = list;
    for (Tree child : children) {
      child.setParent(this);
    }
  }

  @Override
  public void setLabel(String s) {
    this.label = s;
  }


  @Override
  public void setLength(int i) {

  }

  @Override
  public void setType(Type type) {

  }

  @Override
  public void setParentAndUpdateChildren(Tree tree) {
    if (this.parent != null)
      this.parent.getChildren().remove(this);
    this.parent = tree;
    if (this.parent != null)
      parent.getChildren().add(this);
  }

  @Override
  public void addChild(Tree tree) {
    children.add(tree);
    tree.setParent(this);
  }

  @Override
  public void insertChild(Tree tree, int i) {
    children.add(i, tree);
    tree.setParent(this);
  }

  @Override
  public Object setMetadata(String s, Object o) {
    return null;
  }

  @Override
  public void setPos(int i) {

  }

  // Other methods

  @Override
  public Tree deepCopy() {
    Tree copy = new AlloyAST(this);
    for (Tree child : this.getChildren()) {
      copy.addChild(child.deepCopy());
    }
    return copy;
  }

  /**
   * Returns true if the two trees are equal. Two trees are equal if they have the same label and
   * the same children.
   *
   * @param obj The other AST to compare to.
   * @return True if the two AST are equal.
   */
  @Override
  public boolean equals(Object obj) {
    // Null check
    if (obj == null) {
      return false;
    }

    // Not an AlloyAST
    if (!(obj instanceof AlloyAST other)) {
      return false;
    }

    // Label check
    if (!this.label.equals(other.label)) {
      return false;
    }

    // Number of children check
    if (this.children.size() != other.children.size()) {
      return false;
    }

    // Children check
    for (int i = 0; i < this.children.size(); i++) {
      Tree child = this.children.get(i),
              otherChild = other.children.get(i);
      if (!child.equals(otherChild)) {
        return false;
      }
    }

    return true;
  }
}


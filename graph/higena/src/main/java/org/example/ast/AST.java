package org.example.ast;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeMetrics;
import com.github.gumtreediff.tree.Type;

import java.util.*;

public class AST implements Tree {
  List<Tree> children = new ArrayList<>();
  private Tree parent;
  private String label;

  public AST(Node<StringNodeData> root) {
    this.setLabel(root.getNodeData().getLabel());

    for (Node<StringNodeData> child : root.getChildren()) {
      this.addChild(new AST(child));
    }
  }

  public AST(Tree other) {
    this.label = other.getLabel();
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
  public void setChildren(List<Tree> list) {
    this.children = list;
    for (Tree child : list) {
      child.setParent(this);
    }
  }

  @Override
  public List<Tree> getChildren() {
    return children;
  }

  @Override
  public void setParent(Tree tree) {
    this.parent = tree;
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
  public Tree getParent() {
    return parent;
  }

  @Override
  public Tree deepCopy() {
    Tree copy = new AST(this);
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
  public TreeMetrics getMetrics() {
    return null;
  }

  @Override
  public void setMetrics(TreeMetrics treeMetrics) {

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
}


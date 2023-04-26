package org.higena.ast;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.tree.AbstractTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class AST extends AbstractTree {
  private String label;

  public AST(Node<StringNodeData> root) {
    this.children = new ArrayList<>();
    this.setLabel(root.getNodeData().getLabel());

    for (Node<StringNodeData> child : root.getChildren()) {
      this.addChild(new AST(child));
    }
  }

  public AST(Tree other) {
    this.children = new ArrayList<>();
    this.label = other.getLabel();
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


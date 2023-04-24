package org.higena.ast.matchers;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import org.higena.ast.AST;

public abstract class TreeMatcher implements Matcher {

public TreeMatcher() {}

  public MappingStore match(Node<StringNodeData> t1, Node<StringNodeData> t2) {
    AST src = new AST(t1);
    AST dst = new AST(t2);

    return this.match(src, dst, new MappingStore(src, dst));
  }

  @Override
  public abstract MappingStore match(Tree src, Tree dst, MappingStore mappingStore);
}

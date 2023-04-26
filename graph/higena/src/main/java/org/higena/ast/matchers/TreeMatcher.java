package org.higena.ast.matchers;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.matchers.MappingStore;

public abstract class TreeMatcher {

  public TreeMatcher() {
  }

  public abstract MappingStore match(Node<StringNodeData> t1,
                                     Node<StringNodeData> t2);

}

package org.higena.ast.matchers;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import org.higena.ast.AST;

/**
 * This class matches two trees using the edit mapping computed by the GumTree
 * recursive algorithm.
 * The mapping is then used to generate an edit script using Chawathe's algorithm.
 */
public class GumTreeMatcher extends TreeMatcher{

  public GumTreeMatcher() {}

  public MappingStore match(Node<StringNodeData> t1,
                            Node<StringNodeData> t2) {
    AST src = new AST(t1), dst = new AST(t2);
    System.out.println(src.toTreeString());
    Matcher defaultMatcher = Matchers.getInstance().getMatcher();
    return defaultMatcher.match(src, dst);
  }
}

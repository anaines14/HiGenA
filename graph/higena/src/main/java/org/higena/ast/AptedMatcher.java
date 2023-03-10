package org.higena.ast;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;

import java.util.List;

/**
 * This class matches two trees using the edit mapping computed by the TED algorithm.
 * The mapping is then used to generate an edit script using Chawathe's algorithm.
 */
public class AptedMatcher implements Matcher {
  private final TED ted;

  public AptedMatcher(TED ted) {
    this.ted = ted;
  }

  public MappingStore match(Node<StringNodeData> t1, Node<StringNodeData> t2) {
    AST src = new AST(t1);
    AST dst = new AST(t2);

    return this.match(src, dst, new MappingStore(src, dst));
  }

  @Override
  public MappingStore match(Tree src, Tree dst, MappingStore mappingStore) {
    List<int[]> arrayMappings = this.ted.computeEdits();
    List<Tree> srcs = TreeUtils.postOrder(src);
    List<Tree> dsts = TreeUtils.postOrder(dst);

    for (int[] m : arrayMappings) {
      if (m[0] != 0 && m[1] != 0) {
        Tree srcg = srcs.get(m[0] - 1);
        Tree dstg = dsts.get(m[1] - 1);

        if (mappingStore.isMappingAllowed(srcg, dstg))
          mappingStore.addMapping(srcg, dstg);
      }
    }
    return mappingStore;
  }
}

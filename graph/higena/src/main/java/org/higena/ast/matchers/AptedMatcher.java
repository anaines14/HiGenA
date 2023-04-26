package org.higena.ast.matchers;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import org.higena.ast.AST;
import org.higena.ast.TED;

import java.util.List;

/**
 * This class matches two trees using the edit mapping computed by the APTED
 * algorithm.
 * The mapping is then used to generate an edit script using Chawathe's algorithm.
 */
public class AptedMatcher extends TreeMatcher {
  private final TED ted;

  public AptedMatcher(TED ted) {
    this.ted = ted;
  }

  public MappingStore match(Node<StringNodeData> t1, Node<StringNodeData> t2) {
    AST src = new AST(t1);
    AST dst = new AST(t2);
    MappingStore ms = new MappingStore(src, dst);

    List<int[]> arrayMappings = this.ted.computeEdits();
    List<Tree> srcs = TreeUtils.postOrder(src);
    List<Tree> dsts = TreeUtils.postOrder(dst);

    for (int[] m : arrayMappings) {
      if (m[0] != 0 && m[1] != 0) {
        Tree srcg = srcs.get(m[0] - 1);
        Tree dstg = dsts.get(m[1] - 1);

        if (ms.isMappingAllowed(srcg, dstg))
          ms.addMapping(srcg, dstg);
      }
    }

    return ms;
  }

}

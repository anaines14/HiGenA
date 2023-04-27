package org.higena.ast.matchers;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import org.higena.ast.AlloyAST;
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

  public MappingStore match(AlloyAST t1, AlloyAST t2) {
    MappingStore ms = new MappingStore(t1, t2);

    List<int[]> arrayMappings = this.ted.computeEdits();
    List<Tree> srcs = TreeUtils.postOrder(t1);
    List<Tree> dsts = TreeUtils.postOrder(t2);

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

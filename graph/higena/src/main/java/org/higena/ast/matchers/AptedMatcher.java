package org.higena.ast.matchers;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
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

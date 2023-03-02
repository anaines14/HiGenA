package org.example.ast;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import org.example.ast.AST;
import org.example.ast.TED;

import java.util.List;

public class AptedMatcher implements Matcher {
  private TED ted;

  public MappingStore match(TED ted, String tree1, String tree2) {
    this.ted = ted;
    ted.computeEditDistance(tree1, tree2); // must run before
    AST src = new AST(ted.getTree1());
    AST dst = new AST(ted.getTree2());

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

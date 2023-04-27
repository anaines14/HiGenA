package org.higena.ast.matchers;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import org.higena.ast.AlloyAST;

/**
 * This class matches two trees using the edit mapping computed by the GumTree
 * recursive algorithm.
 * The mapping is then used to generate an edit script using Chawathe's algorithm.
 */
public class GumTreeMatcher extends TreeMatcher {

  public GumTreeMatcher() {
  }

  public MappingStore match(AlloyAST t1, AlloyAST t2) {
    Matcher defaultMatcher = Matchers.getInstance().getMatcher();
    return defaultMatcher.match(t1, t2);
  }
}

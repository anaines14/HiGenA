package org.higena.ast.matchers;

import com.github.gumtreediff.matchers.MappingStore;
import org.higena.ast.AlloyAST;

public abstract class TreeMatcher {

  public TreeMatcher() {
  }

  public abstract MappingStore match(AlloyAST t1,
                                     AlloyAST t2);

}

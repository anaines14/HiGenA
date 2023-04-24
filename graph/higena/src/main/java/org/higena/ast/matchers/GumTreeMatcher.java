package org.higena.ast.matchers;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;

/**
 * This class matches two trees using the edit mapping computed by the GumTree
 * recursive algorithm.
 * The mapping is then used to generate an edit script using Chawathe's algorithm.
 */
public class GumTreeMatcher extends TreeMatcher{

  public GumTreeMatcher() {}

  @Override
  public MappingStore match(Tree src, Tree dst, MappingStore mappingStore) {
    if (mappingStore.isMappingAllowed(src, dst))
      mappingStore.addMappingRecursively(src, dst);
    return mappingStore;
  }
}

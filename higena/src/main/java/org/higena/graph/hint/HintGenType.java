package org.higena.graph.hint;

/**
 * Hint generation types.
 * TED: Uses the tree edit distance between ASTs as edge weight.
 * REL_POISSON: Uses the poisson distribution with the popularity of edges as
 * edge weight. Example: If an edge was used 10 times, it
 * has a weight of 1/10.
 * NODE_POISSON: Uses the poisson distribution with the popularity of nodes as
 * edge weight. Example: If a node was visited 10 times, the edge that leads to
 * it has a weight of 1/10.
 */
public enum HintGenType {
  TED("ted"), REL_POISSON("poisson"), NODE_POISSON("dstPoisson");

  private final String property;

  HintGenType(String property) {
    this.property = property;
  }

  @Override
  public String toString() {
    return property;
  }
}

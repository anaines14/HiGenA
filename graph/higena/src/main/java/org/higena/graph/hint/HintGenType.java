package org.higena.graph.hint;

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

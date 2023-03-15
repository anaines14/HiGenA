package org.higena;

import org.higena.graph.Graph;

public class Main {
  public static void main(String[] args) {
    Graph graph = new Graph();
    //graph.setup();
    String ast = graph.parseExpr("no File");
    graph.getDijkstraHint(ast);

  }
}
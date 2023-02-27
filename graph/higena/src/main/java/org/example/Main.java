package org.example;

public class Main {
  public static void main(String[] args) {
    Graph graph = new Graph();
    graph.setup();
    graph.aggregateEqualNodes();
    graph.removeEquivNodes();
  }

}
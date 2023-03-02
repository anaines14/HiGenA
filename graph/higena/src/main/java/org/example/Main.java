package org.example;

import org.example.graph.Graph;

public class Main {
  public static void main(String[] args) {
    prepareData();
  }

  public static void prepareData() {
    Graph graph = new Graph();
    graph.setup();
  }

}
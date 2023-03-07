package org.example;

import org.example.ast.TED;
import org.example.graph.Graph;

public class Main {
  public static void main(String[] args) {
    Graph graph = new Graph();
    graph.setup();
    graph.addOpToEdges();
/*
    TED ted = new TED();
    String tree1 = "{a{b{d}{e{f}}}{c}}";
    String tree2 = "{a{b}{c{f}}{d{e}}}";

    //ted.getEdits(tree1, tree2);*/
  }
}
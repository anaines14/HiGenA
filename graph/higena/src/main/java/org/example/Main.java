package org.example;

public class Main {
  public static void main(String[] args) {
    //Graph graph = new Graph();
    //graph.setup();
    //graph.mergeEquivNodes("ast");
    TED.getEdits("{AND{no{this/Protected}}{no{this" +
            "/Trash}}}", "{AND{no{this/Trash}}{no{this/Protected}}}");

  }

}
package org.higena;

import org.higena.graph.Graph;

public class Main {

  /**
   * Setups the database passed as argument.
   * @param args The arguments passed to the program.
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: java -jar higena.jar <challenge> <predicate>");
      System.exit(1);
    }

    String challenge = args[0];
    String predicate = args[1];

    Graph graph = new Graph(challenge, predicate);
    graph.setup();
  }
}

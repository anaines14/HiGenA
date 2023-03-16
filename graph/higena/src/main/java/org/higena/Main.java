package org.higena;

import org.higena.graph.Graph;
import org.neo4j.driver.types.Relationship;

public class Main {
  public static void main(String[] args) {
    Graph graph = new Graph("9jPK8KBWzjFmBx4Hb", "prop1");
    //graph.setup();

    String ast = graph.parseExpr("");
    Relationship rel = graph.getTEDHint(ast);

    System.out.println("Edge ID: " + rel.get("id"));
    System.out.println("Operations: " + rel.get("operations"));
    System.out.println("TED: " + rel.get("ted"));

  }
}
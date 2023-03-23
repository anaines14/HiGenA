package org.higena;

import org.higena.ast.actions.EditAction;
import org.higena.graph.Graph;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;

import java.util.List;

public class Main {
  public static void main(String[] args) {
    Graph graph = new Graph("9jPK8KBWzjFmBx4Hb", "prop1");
    graph.setup();
/*
    Relationship rel = graph.getTEDHint("always (no File and no Protected)");


    System.out.println("Edge ID: " + rel.get("id"));
    System.out.println("Operations: " + rel.get("operations"));
    System.out.println("TED: " + rel.get("ted"));


    for (String op : rel.get("operations").asList(Value::asString)) {
      System.out.println(EditAction.fromString(op));
    }
*/
  }
}
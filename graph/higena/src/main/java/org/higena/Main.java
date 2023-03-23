package org.higena;

import org.higena.ast.actions.EditAction;
import org.higena.graph.Graph;
import org.higena.graph.Hint;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Relationship;

import java.util.List;

public class Main {
  public static void main(String[] args) {
    Graph graph = new Graph("9jPK8KBWzjFmBx4Hb", "prop1");
    //graph.setup();

    Hint hint = graph.getTEDHint("always (no File and no Protected)");
    System.out.println("Hint:\n" + hint.toHintMsg());

  }
}
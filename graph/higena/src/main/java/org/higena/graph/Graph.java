package org.higena.graph;

import io.github.cdimascio.dotenv.Dotenv;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.List;

/**
 * Wrapper class for the database (Db class).
 */
public class Graph {
  private final String uri, user, password, databaseName, challenge, predicate;

  public Graph() {
    Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();

    this.uri = dotenv.get("NEO4J_URI");
    this.user = dotenv.get("NEO4J_USERNAME");
    this.password = dotenv.get("NEO4J_PASSWORD");
    this.databaseName = "ajpkk";
    this.challenge = "9jPK8KBWzjFmBx4Hb";
    this.predicate = "prop1";

    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      db.verifyConnection();
    }
  }

  /**
   * Creates a new database and sets up the graph.
   */
  public void setup() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      System.out.println("Starting DB setup...");
      db.setup();
      System.out.println("Success: Finished setup.\n");
    }
  }

  public void getHint(String nodeId) {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      Result res = db.dijkstra(nodeId);
      try {
        // Get the two first nodes in the path
        List<Node> rels = res.single().get("path").asList(Value::asNode);
        Node n1 = rels.get(0), n2 = rels.get(1);
        // Get the relationship between the two nodes
        Relationship rel = db.getRelationship(n1, n2);

        System.out.println("Edge ID: " + rel.get("id"));
        System.out.println("Operations: " + rel.get("operations"));
        System.out.println("TED: " + rel.get("ted"));

      } catch (NoSuchRecordException e) {
        System.out.println("ERROR: Cannot retrieve hint.");
      }
    }
  }
}

package org.higena.graph;

import io.github.cdimascio.dotenv.Dotenv;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

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

  public void getHint() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      Result res = db.dijkstra("9jPK8KBWzjFmBx4Hb");
      Record rec = res.single();
      Path path = rec.get("path").asPath();
    }
  }
}

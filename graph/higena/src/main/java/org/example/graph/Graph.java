package org.example.graph;

import io.github.cdimascio.dotenv.Dotenv;
import org.neo4j.driver.Result;

public class Graph {
  private final String uri;
  private final String user;
  private final String password;
  private final String database;

  public Graph() {
    Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();

    this.uri = dotenv.get("NEO4J_URI");
    this.user = dotenv.get("NEO4J_USERNAME");
    this.password = dotenv.get("NEO4J_PASSWORD");
    this.database = "ajPKK";

    try (Db db = new Db(uri, user, password, database)) {
      db.verifyConnection();
    }
  }

  /**
   * Creates a new database and sets up the graph.
   */
  public void setup() {
    try (Db db = new Db(uri, user, password, database)) {
      System.out.println("START SETUP");
      db.addDb(db.getName());
      db.setup();
      System.out.println("END SETUP\n");
    }
  }

  public void addOpToEdges() {
    try (Db db = new Db(uri, user, password, database)) {
      db.addTEDToEdges();
    }
  }
}

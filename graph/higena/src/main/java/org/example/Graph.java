package org.example;

import io.github.cdimascio.dotenv.Dotenv;

public class Graph {
  private Db db;

  public Graph() {
    setup();
  }

  public void setup() {
    Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();

    String uri = dotenv.get("NEO4J_URI");
    String user = dotenv.get("NEO4J_USERNAME");
    String password = dotenv.get("NEO4J_PASSWORD");
    String database = "ajPKK";

    try (Db db = new Db(uri, user, password, database)) {
      this.db = db;
      db.verifyConnection();
      db.createDb(database);
      db.setup();
      System.out.println("Finished setup successfully.");
    }
  }
}

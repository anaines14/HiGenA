package org.example;

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
   * Removes nodes with repeated expression from the graph.
   * Finally, for each component, it keeps the
   * first node and removes the rest.
   */
  public void removeEquivNodes() {
    try (Db db = new Db(uri, user, password, database)) {
      Result res = db.getDistinctPropertyValues("componentId");
      while (res.hasNext()) {
        int component = res.next().get("componentId").asInt();
        Result nodes = db.getNodesWithPropertyValue("componentId", component);
        System.out.println(nodes.list());
      }
    }
  }

  /**
   * Creates edges between nodes with the same expression. Then, runs the
   * Weakly Connected Components algorithm to find the connected components
   * of the graph. Each node gets a componentId property with the id of the
   * component it belongs to.
   */
  public void aggregateEqualNodes() {
    String projectionName = "equalGraph";
    String relName = "EQUAL";
    try (Db db = new Db(uri, user, password, database)) {
      // Create edges between nodes with the same expression
      db.runQuery("""
              MATCH (n:Submission)
              MATCH (s:Submission)
              WHERE n._id <> s._id AND n.expr = s.expr
              MERGE (n)-[:%s]-(s)""".formatted(relName));
      // Check if graph already exists
      if (!db.hasProjection(projectionName)) {
        // Create graph projection
        db.addProjection(projectionName, "Submission", relName);
      }
      // Run Weakly Connected Components algorithm
      db.runConnectedComponents(projectionName, "componentId");
      // Drop graph projection
      db.deleteProjection(projectionName);
      // Drop edges
      //db.deleteEdges(relName);
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
}

package org.example;

import org.neo4j.driver.*;

public class Db implements AutoCloseable {
  private final Driver driver;
  private final Session session;
  private final String name;

  public Db(String uri, String user, String password, String database) {
    this.name = database;
    driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    session = driver.session(SessionConfig.forDatabase(database));
  }

  public void setup() {
    deleteAllNodes();
    addUniqueConstraint();
    addNodes("9jPK8KBWzjFmBx4Hb", "prop1");
    addEdges();
    deleteProperty("derivationOf");
    addCorrectLabel();
    addIncorrectLabel();
  }

  public Result runQuery(String query) {
    return session.run(new Query(query));
  }

  public void close() throws RuntimeException {
    driver.close();
  }

  public void verifyConnection() {
    driver.verifyConnectivity();
  }

  public boolean hasProjection(String name) {
    Result res = runQuery("CALL gds.graph.exists('" + name + "') YIELD exists");
    return res.single().get("exists").asBoolean();
  }

  public void runConnectedComponents(String graphName, String writeProperty) {
    runQuery("CALL gds.wcc.write('" + graphName + "', {writeProperty: '" +
            writeProperty + "'}) " + "YIELD " +
            "nodePropertiesWritten, componentCount");
  }

  // CREATE methods

  public void addDb(String name) {
    runQuery("CREATE DATABASE " + name + " IF NOT EXISTS");
  }

  public void addUniqueConstraint() {
    runQuery("""
            CREATE CONSTRAINT UniqueSubmission
            IF NOT EXISTS
            FOR (s:Submission)
            REQUIRE s._id IS UNIQUE""");
    System.out.println("Created unique constraint.");
  }

  public void addNodes(String challenge, String pred) {
    Result res = runQuery("LOAD CSV WITH HEADERS FROM 'file:///" + challenge +
            "/" + pred + ".csv' AS row\n" +
            "MERGE (s:Submission {\n" +
            "\t_id: row._id,\n" +
            "\tcmd_n: row.cmd_n,\n" +
            "\tcode: row.code,\n" +
            "\tderivationOf: row.derivationOf,\n" +
            "\tsat: toInteger(row.sat),\n" +
            "\texpr: row.expr,\n" +
            "\tast: row.ast\n" +
            "})\n" +
            "RETURN count(s)\n");

    System.out.println("Created " + res.consume().counters().nodesCreated() + " nodes.");
  }

  public void addEdges() {
    Result res = runQuery("""
            MATCH (s:Submission)
            MATCH (d:Submission)
            WHERE s._id = d.derivationOf AND s._id <> d._id
            MERGE (s)-[r:Derives]->(d)
            RETURN count(r)""");
    System.out.println("Created " + res.consume().counters().relationshipsCreated() +
            " Derives edges.");
  }

  public void addCorrectLabel() {
    Result res = runQuery("""
            MATCH (s:Submission {sat: 1})
            SET s:Correct
            RETURN count(s)""");
    System.out.println("Set " + res.consume().counters().labelsAdded() + " " +
            "Correct labels.");
  }

  public void addIncorrectLabel() {
    Result res = runQuery("""
            MATCH (s:Submission {sat: 0})
            SET s:Incorrect
            RETURN count(s)""");
    System.out.println("Set " + res.consume().counters().labelsAdded() + " " +
            "Incorrect labels.");
  }

  public void addProjection(String name, String label, String relationship) {
    runQuery("CALL gds.graph.project('%s', '%s', '%s')"
            .formatted(name, label, relationship));
  }

  // DELETE methods

  public void deleteAllNodes() {
    runQuery("MATCH (n) DETACH DELETE n");
    System.out.println("Delete all nodes.");
  }

  public void deleteProperty(String property) {
    Result res = runQuery("MATCH (s:Submission)\n" +
            "REMOVE s." + property + "\n" +
            "RETURN count(s)");
    System.out.println("Removed " + res.consume().counters().propertiesSet() +
            " " + property + " properties.");
  }

  public void deleteProjection(String name) {
    runQuery("CALL gds.graph.drop('" + name + "')");
  }

  public void deleteEdges(String relationship) {
    Result res = runQuery("MATCH ()-[r:" + relationship + "]-()\n" +
            "DELETE r\n" +
            "RETURN count(r)");
    System.out.println("Deleted " + res.consume().counters().relationshipsDeleted() + " edges.");
  }

  public void deleteDb(String name) {
    runQuery("DROP DATABASE " + name + " IF EXISTS");
  }

  // GET methods

  public Result getNodesWithPropertyValue(String property, int value) {
    return runQuery("MATCH (s:Submission)\n" +
            "WHERE s." + property + " = '" + value + "'\n" +
            "RETURN s");
  }

  public Result getDistinctPropertyValues(String property) {
    return runQuery("MATCH (s:Submission)\n" +
            "RETURN DISTINCT s." + property + " AS " + property);
  }

  public String getName() {
    return name;
  }
}

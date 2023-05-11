package org.higena.graph;

import org.higena.ast.TED;
import org.higena.ast.actions.TreeDiff;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles the database connection and operations.
 */
public class Db implements AutoCloseable {
  private final Driver driver; // Neo4j driver
  private final String challenge; // Challenge name
  private final String predicate; // Predicate name
  private Session session; // Neo4j session
  private String name; // Database name

  public Db(String uri, String user, String password, String challenge, String predicate) {
    this(uri, user, password, "neo4j", challenge, predicate);
  }

  public Db(String uri, String user, String password, String databaseName, String challenge, String predicate) {
    this.name = databaseName;
    this.challenge = challenge;
    this.predicate = predicate;
    driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password)); // Create driver
    session = driver.session(SessionConfig.forDatabase(name)); // Connect to the database
  }

  /**
   * Performs a sequence of actions to prepare the database.
   * 1. Cleans the database by deleting all nodes and edges and projections.
   * 2. Adds unique constraints to avoid duplicate IDs.
   * 3. Adds nodes to the database.
   * 4. Add edges to the database.
   * 5. Adds the correct and incorrect labels to the nodes.
   * 6. Deletes the derivationOf and sat properties from the nodes.
   * 7. Adds popularity to the edges.
   * 8. Adds nodes popularity by aggregating equivalent nodes.
   * 8. Adds the TED and edit operations to the edges.
   * 9. Adds the Poisson distribution to the edges.
   */
  public void setup() throws ClientException {
    deleteAllNodes();
    deleteAllProjections();
    addUniqueConstraints();
    addSubmissionNodes();
    addDerivationEdges();
    addSubmissionLabels();
    deleteProperty("derivationOf");
    deleteProperty("sat");
    addEdgesPopularity();
    aggregateEquivNodes();
    addTreeDiffToEdges();
    addNodePoissonToEdges();
  }

  // Algorithms

  /**
   * Runs the Dijkstra's algorithm using the given weight property to find the
   * shortest path between the source node and a Correct node.
   *
   * @param sourceId       ID of the source node.
   * @param weightProperty Property to use as weight.
   * @return Result of the dijkstra algorithm. Contains the index, source
   * node ID, target node ID, total cost, node IDs in the path, costs and
   * sequence of nodes in the path.
   */
  public Result runDijkstra(String sourceId, String weightProperty) {
    String projectionName = "dijkstra|" + weightProperty;
    // Update projection if it exists
    if (hasProjection(projectionName)) deleteProjection(projectionName);
    addProjection(projectionName, "Submission", "Derives", weightProperty);
    // Run Dijkstra's algorithm
    return runQuery(String.format(
            "MATCH (source:Submission {id: \"%s\"}), (target:Correct)\n" +
                    "WHERE source.id <> target.id\n" +
                    "CALL gds.shortestPath.dijkstra.stream('%s', {\n" +
                    "    sourceNode: source,\n" +
                    "    targetNode: target,\n" +
                    "    relationshipWeightProperty: '%s'\n" +
                    "})\n" +
                    "YIELD totalCost, path\n" +
                    "RETURN\n" +
                    "    totalCost,\n" +
                    "    nodes(path) AS path,\n" +
                    "    relationships(path) AS rels\n" +
                    "ORDER BY totalCost\n" +
                    "LIMIT 1",
            sourceId,
            projectionName,
            weightProperty));
  }

  /**
   * Creates edges between nodes with the same property. Then, runs the
   * Weakly Connected Components algorithm to find the connected components
   * of the graph. Each node gets a componentId property with the id of the
   * component it belongs to.
   */
  private void aggregateEquivNodes() {
    String projectionName = "equalGraph", relName = "EQUAL", componentProperty = "componentId";
    // Create edges between nodes with the same property
    runQuery(String.format(
            "MATCH (n:Submission), (s:Submission)\n" +
                    "WHERE n.id <> s.id AND n.ast = s.ast\n" +
                    "MERGE (n)-[:%s]-(s)", relName));
    // Check if graph already exists and create it if it doesn't
    if (!hasProjection(projectionName)) {
      addProjection(projectionName, relName);
    }
    // Run Weakly Connected Components algorithm
    try {
      runConnectedComponents(projectionName, componentProperty);
      deleteProjection(projectionName);
      deleteEdges(relName);
      // Get components
      List<Record> components = getDistinctPropertyValues(componentProperty).list();
      addNodesPopularity(components);
      // Aggregate nodes: delete equivalent nodes except for one
      deleteEquivNodes(components);
    } catch (ClientException e) {
      System.out.println("No equivalent nodes found so no aggregation was done.");
    }
  }

  // ADD Methods

  /**
   * Creates a relationship Derives between the given nodes. Each edge has a
   * TED property with the TED between the nodes and an operations property
   * with the edit operations needed to transform the source node into the
   * target node. Each edge has also a popularity property with the number of
   * times the edge appears in the database, a poisson property with the
   * Poisson distribution of the edge's popularity and a dstPoisson property
   * with the Poisson distribution of the target node's popularity.
   *
   * @param n1 Node 1
   * @param n2 Node 2
   * @return Relationship created.
   */
  public Relationship addEdge(Node n1, Node n2) {
    String ast1 = n1.get("ast").asString(), ast2 = n2.get("ast").asString();
    TED ted = new TED();
    TreeDiff diff = ted.computeTreeDiff(ast1, ast2);

    String query = String.format(
            "MATCH (n1:Submission {id: '%s'}), (n2:Submission {id: '%s'})\n" +
                    "MERGE (n1)-[r:Derives {\n" +
                    "    id: randomUUID(),\n" +
                    "    ted: %d,\n" +
                    "    operations: %s,\n" +
                    "    popularity: 0,\n" +
                    "    poisson: 1.5,\n" +
                    "    dstPoisson:\n" +
                    "    CASE\n" +
                    "        WHEN n2.popularity = 0 THEN 1.5\n" +
                    "        ELSE 1.0 / n2.popularity\n" +
                    "    END\n" +
                    "}]->(n2)\n" +
                    "RETURN r AS edge", n1.get("id").asString(), n2.get("id").asString(), diff.getTed(), diff.getActions());

    return runQuery(query).single().get(0).asRelationship();
  }

  /**
   * Creates an incorrect node in the graph with the given properties.
   * Popularity is set to 1.0.
   *
   * @param expr Expression of the node
   * @param ast  AST of the node
   * @return The created node.
   */
  public Node addIncorrectNode(String expr, String ast, String code) {

    Result res = runQuery(String.format(
            "CREATE (n:Submission:Incorrect {id: randomUUID(),\n" +
                    "    code: '%s',\n" +
                    "    ast: '%s',\n" +
                    "    expr: '%s',\n" +
                    "    popularity: 1.0})\n" +
                    "RETURN n AS node", code, ast, expr));

    return res.single().get(0).asNode();
  }

  /**
   * Adds a property to all Derives edges called dstPoisson with the value
   * 1.0 / popularity of the destination node for calculating the poisson path.
   */
  private void addNodePoissonToEdges() {
    runQuery(
            "MATCH ()-[r:Derives]->(dst:Submission)\n" +
                    "SET r.dstPoisson = \n" +
                    "CASE\n" +
                    "    WHEN dst.popularity = 0 THEN 1.5\n" +
                    "    ELSE 1.0 / dst.popularity\n" +
                    "END");
    System.out.println("Added node popularity to edges");
  }

  /**
   * Adds TED property and operations property to all Derives edges.
   * TED is the edit distance between the source and destination nodes.
   * Operations is the list of edit operations needed to transform the source
   * node into the destination node.
   */
  private void addTreeDiffToEdges() {
    // Get all edges and its nodes
    Result res = runQuery(
            "MATCH (src:Submission)-[e:Derives]->(dst:Submission)\n" +
                    "RETURN src.ast AS src, dst.ast AS dst, e.id AS edgeID");
    TED ted = new TED();

    while (res.hasNext()) {
      // Get source and destination nodes of the edge + edge itself
      Record rec = res.next();
      String srcAST = rec.get("src").asString(), dstAST = rec.get("dst").asString(), edge = rec.get("edgeID").asString();

      // Compute tree differences (edit distance and edits)
      TreeDiff diff = ted.computeTreeDiff(srcAST, dstAST);
      // Update edge
      runQuery(String.format(
              "MATCH ()-[e:Derives]->()\n" +
                      "WHERE e.id = '%s'\n" +
                      "SET e.ted = %d\n" +
                      "SET e.operations = %s", edge, diff.getTed(), diff.getActions()));
    }
  }

  /**
   * Creates a new database with the given name if it does not exist.
   * Switches to the new database by closing the current session and opening
   * a new one with the correct configuration.
   *
   * @param databaseName Name of the database to create.
   */
  public void addDb(String databaseName) {
    // Create database
    runQuery("CREATE DATABASE " + databaseName + " IF NOT EXISTS");

    // Switch to new database
    this.name = databaseName;
    session.close();
    session = driver.session(SessionConfig.forDatabase(name));
  }

  /**
   * Adds constraint to ensure that each Submission node has a unique id
   * property.
   */
  public void addUniqueConstraints() {
    Result res = runQuery("CREATE CONSTRAINT UniqueSubmission IF NOT EXISTS FOR " +
            "(s:Submission) REQUIRE s.id IS UNIQUE");
    System.out.println("Added " + res.consume().counters().constraintsAdded() + " unique node.id constraint(s).");
  }

  /**
   * Loads nodes from a csv file with Alloy4Fun submissions into the database.
   * The csv file must have the following columns: _id, code,
   * derivationOf, sat, expr, ast. The derivationOf, expr and ast columns
   * can be empty. The id column must be unique. The sat column must be
   * either 0 or 1.
   */
  private void addSubmissionNodes() throws ClientException {
    Result res = runQuery(
            "LOAD CSV WITH HEADERS FROM 'file:///datasets/" + this.challenge + "/" + this.predicate + ".csv' AS row\n" +
                    "MERGE (s:Submission {\n" +
                    "  id: row._id,\n" +
                    "  code: row.code,\n" +
                    "  derivationOf: CASE WHEN row.derivationOf IS NULL THEN '' ELSE row.derivationOf END,\n" +
                    "  sat: toInteger(row.sat),\n" +
                    "  expr: CASE WHEN row.expr IS NULL THEN '' ELSE row.expr END,\n" +
                    "  ast: CASE WHEN row.ast IS NULL THEN '' ELSE row.ast END\n" +
                    "})\n" +
                    "RETURN count(s) AS count");

    System.out.println("Created " + res.consume().counters().nodesCreated() + " nodes.");
  }

  /**
   * Creates directed Derives edges between nodes where the derivationOf
   * property of the source node matches the id property of the target node.
   */
  private void addDerivationEdges() {
    Result res = runQuery(
            "MATCH (s:Submission), (d:Submission)\n" +
                    "WHERE s.id = d.derivationOf AND s.id <> d.id\n" +
                    "MERGE (s)-[r:Derives {id: randomUUID()}]->(d)\n" +
                    "RETURN count(r) AS count");

    System.out.println("Created " + res.consume().counters().relationshipsCreated() + " Derives edges.");
  }

  /**
   * Adds a popularity property to all Derives edges. Popularity is the number
   * of other edges that have the same source and destination nodes. Also
   * adds a poisson property to all edges. Poisson is 1.0 / popularity.
   */
  private void addEdgesPopularity() {
    runQuery(
            "MATCH (n:Submission)-[r:Derives]->(s:Submission)\n" +
                    "CALL {\n" +
                    "    WITH n, r, s\n" +
                    "    MATCH (p:Submission)-[e:Derives]->(t:Submission)\n" +
                    "    WHERE n.ast = p.ast AND s.ast = t.ast AND r.id <> e.id\n" +
                    "    RETURN count(e) AS popularity\n" +
                    "}\n" +
                    "SET r.popularity = popularity + 1\n" +
                    "SET r.poisson =\n" +
                    "CASE\n" +
                    "    WHEN r.popularity = 0 THEN 1.5\n" +
                    "    ELSE 1.0 / r.popularity\n" +
                    "END"
    );

    System.out.println("Added popularity property to edges.");
  }


  /**
   * Adds Correct and Incorrect labels to nodes based on the sat property
   * (0 = Correct, 1 = Incorrect).
   */
  private void addSubmissionLabels() {
    String query =
            "MATCH (s:Submission {sat: %d})\n" +
                    "SET s:%s\n" +
                    "RETURN count(s)";

    Result res = runQuery(String.format(query, 0, "Correct"));
    System.out.println("Set " + res.consume().counters().labelsAdded() + " " + "Correct labels.");

    res = runQuery(String.format(query, 1, "Incorrect"));
    System.out.println("Set " + res.consume().counters().labelsAdded() + " " + "Incorrect labels.");
  }

  /**
   * Adds a popularity property for each node based on the number of nodes in the
   * same component.
   *
   * @param components List of existing components
   */
  private void addNodesPopularity(List<Record> components) {
    for (Record component : components) {
      int componentId = component.get("componentId").asInt();
      // Set popularity for each component
      runQuery(String.format(
              "MATCH (n:Submission {componentId: %d})\n" +
                      "WITH count(n) AS popularity\n" +
                      "CALL {\n" +
                      "  WITH popularity\n" +
                      "  MATCH (n:Submission {componentId: %d})\n" +
                      "  SET n.popularity = popularity\n" +
                      "}\n", componentId, componentId));
    }
  }

  /**
   * Creates a graph projection with the given name, label, and relationship.
   * Graph projections are used to run neo4j graph data science algorithms.
   *
   * @param name         Name of the graph projection
   * @param relationship Relationship type of the edges
   */
  private void addProjection(String name, String relationship) {
    runQuery(String.format("CALL gds.graph.project('%s', '%s', '%s')", name, "Submission", relationship));
  }

  private void addProjection(String name, String label, String relationship, String relProperty) {
    runQuery(String.format("CALL gds.graph.project('%s', '%s', '%s', {relationshipProperties: '%s'})", name, label, relationship, relProperty));
  }

  // DELETE methods

  /**
   * Delete all graph projections for the current database.
   */
  private void deleteAllProjections() {
    Result res = runQuery("CALL gds.graph.list()");
    while (res.hasNext()) {
      Record record = res.next();
      String name = record.get("graphName").asString();
      if (record.get("database").asString().equals(this.name))
        deleteProjection(name);
    }
    System.out.println("Deleted all graph projections.");
  }

  /**
   * Deletes all nodes and edges from the database.
   */
  private void deleteAllNodes() {
    Result res = runQuery("MATCH (n) DETACH DELETE n");
    SummaryCounters counters = res.consume().counters();
    System.out.println("Delete all nodes (" + counters.nodesDeleted() + " nodes and " + counters.relationshipsDeleted() + " edges).");
  }

  /**
   * Deletes a property from all nodes.
   *
   * @param property Name of the property to delete
   */
  private void deleteProperty(String property) {
    Result res = runQuery("MATCH (s:Submission)\n" + "REMOVE s." + property);
    System.out.println("Removed " + res.consume().counters().propertiesSet() + " " + property + " properties.");
  }

  /**
   * Deletes the graph projection with the given name.
   *
   * @param name Name of the graph projection
   */
  private void deleteProjection(String name) {
    runQuery("CALL gds.graph.drop('" + name + "')");
  }

  /**
   * Deletes all edges of the given relationship type.
   *
   * @param relationship Relationship type of the edges to delete
   */
  private void deleteEdges(String relationship) {
    Result res = runQuery("MATCH ()-[r:" + relationship + "]-()\n" + "DELETE r\n" + "RETURN count(r)");
    System.out.println("Deleted " + res.consume().counters().relationshipsDeleted() + " edges.");
  }

  /**
   * Deletes all loops of the given relationship type. Loops are edges
   * where the source and target node are the same.
   */
  private void deleteLoops() {
    Result res = runQuery(
            "MATCH (s:Submission)-[r:Derives]->(s:Submission)\n" +
                    "DELETE r\n" +
                    "RETURN count(r)");
    System.out.println("Deleted " + res.consume().counters().relationshipsDeleted() + " loops.");
  }

  /**
   * Deletes equivalent nodes. Equivalent nodes are nodes that belong to the
   * same component after running the WCC algorithm.
   * For each component the first node is kept and all other nodes are deleted.
   * Derivations of deleted nodes are updated to point to the first node of the
   * component.
   *
   * @param components List of existing components
   */
  private void deleteEquivNodes(List<Record> components) {
    for (Record component : components) {
      int componentId = component.get("componentId").asInt();
      // Remove equivalent nodes from each component
      List<String> queries = getDelEquivNodesQueries(componentId);
      for (String query : queries) {
        runQuery(query);
      }
    }
    // Remove resulting loops
    deleteLoops();
    // Delete componentId property
    deleteProperty("componentId");
  }

  // GET methods

  /**
   * Returns the statistics of the database.
   *
   * @return Number of nodes, edges, correct nodes, and incorrect nodes.
   */
  public Result getStatistics() {
    return runQuery(
            "MATCH (s:Submission)\n" +
                    "WITH count(s) AS submissions\n" +
                    "MATCH (c:Correct)\n" +
                    "WITH submissions, count(c) AS corrects\n" +
                    "MATCH (i:Incorrect)\n" +
                    "WITH submissions, corrects, count(i) as incorrects\n" +
                    "MATCH ()-[r:Derives]->()\n" +
                    "WITH submissions, corrects, incorrects, count(r) AS derivations\n" +
                    "RETURN submissions, corrects, incorrects, derivations");
  }

  /**
   * Returns the node with the given ast.
   *
   * @param ast AST of the node.
   * @return Node with the given ast. Null if no node exists.
   */
  public Node getNodeByAST(String ast) {
    Result res = runQuery(
            "MATCH (s:Submission {ast: \"" + ast + "\"})\n" +
                    "RETURN s as node");
    return res.hasNext() ? res.single().get("node").asNode() : null;
  }

  /**
   * Returns the most similar node to the given AST.  The
   * most similar node is different from the given AST and is
   * the node with the smaller TED. The TED is computed using the APTED
   * algorithm. The search starts with the most popular nodes. Upon finding a
   * TED of 1, the search is stopped.
   *
   * @param ast      AST of the node to compare to the existing nodes
   * @param category Category of the nodes to compare to
   * @return Most similar node to the given AST
   */
  public Node getMostSimilarNode(String ast, String category) {
    // Get all nodes ordered by popularity
    Result res = runQuery(
            "MATCH (s:" + category + ")\n" +
                    "RETURN s AS node\n" +
                    "ORDER BY s.popularity DESC");

    TED ted = new TED();
    int minDist = Integer.MAX_VALUE; // Minimum TED found
    Node similarNode = null; // Most similar node found

    while (res.hasNext()) {
      Node curNode = res.next().get("node").asNode(); // Current node
      // Compute TED between n and curNode
      String curAst = curNode.get("ast").asString();
      int curDist = ted.computeEditDistance(ast, curAst);

      // Skip if TED is 0
      if (curDist == 0) {
        continue;
      }

      // Update minDist and similarNode if lower TED found
      if (curDist < minDist) {
        minDist = curDist;
        similarNode = curNode;
        // Stop earlier if TED is 1
        if (minDist == 1) {
          break;
        }
      }
    }
    return similarNode;
  }

  /**
   * Returns the relationship between the given nodes.
   *
   * @param src Source node.
   * @param dst Destination node.
   * @return Relationship between the given nodes.
   */
  public Relationship getRelationship(Node src, Node dst) {
    Result res = runQuery(String.format(
            "MATCH (s:Submission {id: '%s'})-[edge]->(d:Submission {id: '%s'})\n" +
                    "RETURN edge",
            src.get("id").asString(), dst.get("id").asString()));
    return res.single().get("edge").asRelationship();
  }

  /**
   * Returns all distinct values of the given property.
   *
   * @param property Name of the property
   * @return Result object containing all distinct values of the given property
   */
  private Result getDistinctPropertyValues(String property) {
    return runQuery("MATCH (s:Submission)\n" + "RETURN DISTINCT s." + property + " AS " + property);
  }


  /**
   * Auxiliary method to create queries for deleting equivalent nodes.
   *
   * @param componentId ID of the component
   * @return List of queries to delete equivalent nodes where each derivation of
   * a deleted node is updated to point to the first node of the component
   */
  private List<String> getDelEquivNodesQueries(int componentId) {
    List<String> queries = new ArrayList<>();
    // Get all nodes in component and the first node of the component
    String mainQuery =
            "MATCH (n:Submission {componentId: " + componentId + "})\n" +
                    "WITH collect(DISTINCT n) AS compNodes\n" +
                    "WITH compNodes, compNodes[0] AS firstN\n" +
                    "UNWIND compNodes as cN\n";
    String subquery =
            "CALL {\n" +
                    "WITH cN, firstN\n" +
                    "MATCH (%s)-[r:Derives]->(%s)\n" +
                    "WHERE cN <> firstN\n" +
                    "MERGE (%s)-[p:Derives]->(%s)\n" +
                    "SET p.id = randomUUID()\n" +
                    "SET p.popularity = r.popularity\n" +
                    "SET p.poisson = r.poisson\n" +
                    "DELETE r\n" +
                    "}\n";

    // Swap derivations of component nodes to first node
    // example: (s)-[:Derives]->(cN) -> (s)-[:Derives]->(firstN)
    queries.add(mainQuery + String.format(subquery, "o:Submission", "cN", "o", "firstN"));
    queries.add(mainQuery + String.format(subquery, "cN", "o:Submission", "firstN", "o"));

    // Delete all component nodes except first node
    queries.add(mainQuery +
            "CALL {\n" +
            "WITH cN, firstN\n" +
            "MATCH (cN)\n" +
            "WHERE cN <> firstN\n" +
            "DETACH DELETE cN\n" +
            "}\n");
    return queries;
  }

  /**
   * Returns the code of the original submission.
   *
   * @return Code of the original submission
   */
  public String getOriginalCode() throws NoSuchRecordException {
    Result res = runQuery(
            "MATCH (n:Submission {expr: \"\"})\n" +
                    "RETURN n.code AS code");
    return res.single().get("code").asString();
  }

  // RUN methods

  /**
   * Runs a query and returns the result.
   *
   * @param query Query to run
   * @return Result of the query
   */
  public Result runQuery(String query) {
    return session.run(new Query(query));
  }

  /**
   * Runs the weakly connected components algorithm on the graph projection
   * and writes the component id of each node to the indicated property.
   *
   * @param graphName     Name of the graph projection
   * @param writeProperty Name of the property to write the component id to
   * @throws ClientException If the query fails to run in the case where
   *                         there is an invalid relationship projection. For example, when no EQUAL
   *                         relationships exist.
   */
  private void runConnectedComponents(String graphName, String writeProperty) throws ClientException {
    runQuery("CALL gds.wcc.write('" + graphName + "', {writeProperty: '" + writeProperty + "'}) " + "YIELD " + "nodePropertiesWritten, componentCount");
  }

  // CHECK methods

  /**
   * Verifies that the connection to the database is working.
   */
  public void verifyConnection() {
    driver.verifyConnectivity();
  }

  /**
   * Checks if projection exists.
   *
   * @param name Name of the projection
   * @return True if projection exists, false otherwise
   */
  private boolean hasProjection(String name) {
    Result res = runQuery("CALL gds.graph.exists('" + name + "') YIELD exists");
    return res.single().get("exists").asBoolean();
  }

  // Other

  /**
   * Close the driver and session.
   */
  public void close() throws RuntimeException {
    driver.close();
  }
}

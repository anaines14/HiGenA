package org.higena.graph;

import org.higena.ast.TED;
import org.higena.ast.actions.TreeDiff;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles the database connection and operations.
 */
public class Db implements AutoCloseable {
  private final Driver driver;
  private final Session session;
  private final String name;
  private final String challenge;
  private final String predicate;

  public Db(String uri, String user, String password, String databaseName, String challenge, String predicate) {
    this.name = databaseName;
    this.challenge = challenge;
    this.predicate = predicate;
    driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    session = driver.session(SessionConfig.forDatabase(name));
  }

  /**
   * Performs a sequence of actions to prepare the database.
   * 1. Cleans the database by deleting all nodes and edges.
   * 2. Adds unique constraints to avoid duplicate IDs.
   * 3. Adds nodes to the database.
   * 4. Adds edges to the database.
   * 5. Deletes the derivationOf property from the nodes.
   * 6. Adds the correct and incorrect labels to the nodes.
   * 7. Aggregates nodes with the same property.
   * 8. Adds the TED to the edges.
   */
  public void setup() {
    deleteAllNodes();
    addUniqueConstraints();
    addSubmissionNodes();
    addDerivationEdges();
    deleteProperty("derivationOf");
    addSubmissionLabels();
    addEdgesPopularity();
    aggregateEquivNodes("ast");
    addTreeDiffToEdges();
  }

  // Algorithms

  public Result dijkstra(String sourceId) {
    // Create projection if it doesn't exist
    if (!hasProjection("dijkstra"))
      addProjection("dijkstra", "Submission", "Derives", "ted");
    // Run Dijkstra's algorithm
    Result res = runQuery("""
            MATCH (source:Incorrect {id: "%s"})
            MATCH (target:Correct)
            CALL gds.shortestPath.dijkstra.stream('dijkstra', {
                sourceNode: source,
                targetNode: target,
                relationshipWeightProperty: 'ted'
            })
            YIELD index, sourceNode, targetNode, totalCost, nodeIds, costs, path
            RETURN
                index,
                gds.util.asNode(sourceNode).id AS sourceNodeId,
                gds.util.asNode(targetNode).id AS targetNodeId,
                totalCost,
                [nodeId IN nodeIds | gds.util.asNode(nodeId).id] AS nodeIds,
                costs,
                nodes(path) AS path
            ORDER BY totalCost
            LIMIT 1
            """.formatted(sourceId));

    return res;
  }

  /**
   * Creates edges between nodes with the same property. Then, runs the
   * Weakly Connected Components algorithm to find the connected components
   * of the graph. Each node gets a componentId property with the id of the
   * component it belongs to.
   *
   * @param property Name of the property to aggregate by
   */
  public void aggregateEquivNodes(String property) {
    String projectionName = "equalGraph", relName = "EQUAL", componentProperty = "componentId";
    // Create edges between nodes with the same property
    runQuery("""
            MATCH (n:Submission)
            MATCH (s:Submission)
            WHERE n.id <> s.id AND n.%s = s.%s
            MERGE (n)-[:%s]-(s)""".formatted(property, property, relName));
    // Check if graph already exists and create it if it doesn't
    if (!hasProjection(projectionName)) {
      addProjection(projectionName, "Submission", relName);
    }
    // Run Weakly Connected Components algorithm
    runConnectedComponents(projectionName, componentProperty);
    deleteProjection(projectionName);
    deleteEdges(relName);
    // Get components
    List<Record> components = getDistinctPropertyValues(componentProperty).list();
    addPopularity(components);
    // Aggregate nodes: delete equivalent nodes except for one
    deleteEquivNodes(components);
  }


  /**
   * Adds TED property to derives edges.
   */
  public void addTreeDiffToEdges() {
    // Get all edges and its nodes
    Result res = runQuery("""
            MATCH (src:Submission)-[e:Derives]->(dst:Submission)
            RETURN src.ast AS src, dst.ast AS dst, e.id AS edgeID""");
    TED ted = new TED();

    for (Result it = res; it.hasNext(); ) {
      // Get source and destination nodes of the edge + edge itself
      Record rec = it.next();
      String srcAST = rec.get("src").asString(), dstAST = rec.get("dst").asString(), edge = rec.get("edgeID").asString();

      // Compute tree differences (edit distance and edits)
      TreeDiff diff = ted.computeTreeDiff(srcAST, dstAST);
      // Update edge
      runQuery("""
              MATCH ()-[e:Derives]-()
              WHERE e.id = '%s'
              SET e.ted = %d
              SET e.operations = %s""".formatted(edge, diff.getTed(), diff.getActions()));
    }
  }

  // ADD methods

  /**
   * Adds constraint to ensure that each Submission node has a unique id
   * property.
   */
  public void addUniqueConstraints() {
    String query = """
            CREATE CONSTRAINT %s
            IF NOT EXISTS
            FOR %s
            REQUIRE %s.id IS UNIQUE
            """;

    Result res = runQuery(String.format(query, "UniqueSubmission", "(s:Submission)", "s"));
    System.out.println("Added " + res.consume().counters().constraintsAdded() + " unique node.id constraint(s).");

    // TODO: Add when supported by Neo4j
    //res = runQuery(String.format(query, "UniqueDerives", "()-[r:Derives]-()", "r"));
    //System.out.println("Added " + res.consume().counters().constraintsAdded() + " unique edge.id constraint(s).");
  }

  /**
   * Loads nodes from a csv file with Alloy4Fun submissions into the database.
   */
  public void addSubmissionNodes() {
    Result res = runQuery("LOAD CSV WITH HEADERS FROM 'file:///" + this.challenge + "/" + this.predicate + ".csv' AS row\n" + "MERGE (s:Submission {\n" + "\tid: row._id,\n" + "\tcmd_n: row.cmd_n,\n" + "\tcode: row.code,\n" + "\tderivationOf: row.derivationOf,\n" + "\tsat: toInteger(row.sat),\n" + "\texpr: row.expr,\n" + "\tast: row.ast\n" + "})\n" + "RETURN count(s)\n");

    System.out.println("Created " + res.consume().counters().nodesCreated() + " nodes.");
  }

  /**
   * Creates undirected Derives edges between nodes where the derivationOf
   * property of the source node matches the id property of the target node.
   */
  public void addDerivationEdges() {
    Result res = runQuery("""
            MATCH (s:Submission)
            MATCH (d:Submission)
            WHERE s.id = d.derivationOf AND s.id <> d.id
            MERGE (s)-[r:Derives {id: randomUUID()}]->(d)
            RETURN count(r)""");
    System.out.println("Created " + res.consume().counters().relationshipsCreated() + " Derives edges.");
  }

  public void addEdgesPopularity() {
    runQuery("""
            MATCH (n:Submission)-[r:Derives]->(s:Submission)
            CALL {
                WITH n, r, s
                MATCH (p:Submission)-[e:Derives]->(t:Submission)
                WHERE n.ast = p.ast AND s.ast = t.ast AND r.id <> e.id 
                RETURN count(e) AS popularity
            }
            SET r.popularity = popularity + 1""");

    System.out.println("Added popularity property to edges.");
  }


  /**
   * Adds Correct and Incorrect labels to nodes based on the sat property
   * (0 = Correct, 1 = Incorrect).
   */
  public void addSubmissionLabels() {
    String query = """
            MATCH (s:Submission {sat: %d})
            SET s:%s
            RETURN count(s)""";

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
  public void addPopularity(List<Record> components) {
    for (Record component : components) {
      int componentId = component.get("componentId").asInt();
      // Set popularity for each component
      runQuery("""
               MATCH (n:Submission {componentId: %d})
               WITH count(n) AS popularity
               CALL {
                 WITH popularity
                 MATCH (n:Submission {componentId: %d})
                 SET n.popularity = popularity
               }
              """.formatted(componentId, componentId));
    }
  }

  /**
   * Creates a graph projection with the given name, label, and relationship.
   * Graph projections are used to run neo4j graph data science algorithms.
   *
   * @param name         Name of the graph projection
   * @param label        Label of the nodes
   * @param relationship Relationship type of the edges
   */
  public void addProjection(String name, String label, String relationship) {
    runQuery("CALL gds.graph.project('%s', '%s', '%s')".formatted(name, label, relationship));
  }

  /**
   * Creates a graph projection with the given name, label, and relationship.
   * Graph projections are used to run neo4j graph data science algorithms.
   *
   * @param name          Name of the graph projection
   * @param label         Label of the nodes
   * @param relationship  Relationship type of the edges
   * @param relProperty   Property of the relationship
   */
  public void addProjection(String name, String label, String relationship, String relProperty) {
    runQuery(("CALL gds.graph.project('%s', '%s', '%s', " +
            "{relationshipProperties: '%s'})").formatted(name, label, relationship, relProperty));
  }

  // DELETE methods

  /**
   * Deletes all nodes and edges from the database.
   */
  public void deleteAllNodes() {
    Result res = runQuery("MATCH (n) DETACH DELETE n");
    SummaryCounters counters = res.consume().counters();
    System.out.println("Delete all nodes (" + counters.nodesDeleted() + " nodes and " + counters.relationshipsDeleted() + " edges).");
  }

  /**
   * Deletes a property from all nodes.
   *
   * @param property Name of the property to delete
   */
  public void deleteProperty(String property) {
    Result res = runQuery("MATCH (s:Submission)\n" + "REMOVE s." + property);
    System.out.println("Removed " + res.consume().counters().propertiesSet() + " " + property + " properties.");
  }

  /**
   * Deletes the graph projection with the given name.
   *
   * @param name Name of the graph projection
   */
  public void deleteProjection(String name) {
    runQuery("CALL gds.graph.drop('" + name + "')");
  }

  /**
   * Deletes all edges of the given relationship type.
   *
   * @param relationship Relationship type of the edges to delete
   */
  public void deleteEdges(String relationship) {
    Result res = runQuery("MATCH ()-[r:" + relationship + "]-()\n" + "DELETE r\n" + "RETURN count(r)");
    System.out.println("Deleted " + res.consume().counters().relationshipsDeleted() + " edges.");
  }

  /**
   * Deletes all loops of the given relationship type. Loops are edges
   * where the source and target node are the same.
   *
   * @param relationship Relationship type of the loops to delete
   */
  public void deleteLoops(String relationship) {
    Result res = runQuery("MATCH (s:Submission)-[r:" + relationship + "]->" + "(s:Submission)\n" + "DELETE r\n" + "RETURN count(r)");
    System.out.println("Deleted " + res.consume().counters().relationshipsDeleted() + " loops.");
  }

  /**
   * Deletes equivalent nodes. Equivalent nodes are nodes that belong to the
   * same component after running the connected components algorithm.
   * For each component the first node is kept and all other nodes are deleted.
   * Derivations of deleted nodes are updated to point to the first node of the
   * component.
   *
   * @param components List of existing components
   */
  public void deleteEquivNodes(List<Record> components) {
    for (Record component : components) {
      int componentId = component.get("componentId").asInt();
      // Remove equivalent nodes from each component
      List<String> queries = getDelEquivNodesQueries(componentId);
      for (String query : queries) {
        runQuery(query);
      }
    }
    // Remove resulting loops
    deleteLoops("Derives");
    // Delete componentId property
    deleteProperty("componentId");
  }

  // GET methods

  /**
   * Returns the node with the given ast.
   * @param ast AST of the node.
   * @return Node with the given ast.
   */
  public Node getNodeByAST(String ast) {
    Result res = runQuery("""
            MATCH (s:Submission {ast: '%s'})
            RETURN s as node
            """.formatted(ast));
    return res.single().get("node").asNode();
  }

  /**
   * Returns the relationship between the given nodes.
   * @param src Source node.
   * @param dst Destination node.
   * @return Relationship between the given nodes.
   */
  public Relationship getRelationship(Node src, Node dst) {
    Result res = runQuery("""
            MATCH (s:Submission {id: '%s'})-[edge]->(d:Submission {id: '%s'})
            RETURN edge
            """.formatted(src.get("id").asString(), dst.get("id").asString()));
    return res.single().get("edge").asRelationship();
  }

  /**
   * Returns all distinct values of the given property.
   *
   * @param property Name of the property
   * @return Result object containing all distinct values of the given property
   */
  public Result getDistinctPropertyValues(String property) {
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
    String mainQuery = """
            MATCH (n:Submission {componentId: %d})
            WITH collect(DISTINCT n) AS compNodes
            WITH compNodes, compNodes[0] AS firstN
            UNWIND compNodes as cN
            """.formatted(componentId), subquery = """
            CALL {
                WITH cN, firstN
                MATCH (%s)-[r:Derives]->(%s)
                WHERE cN <> firstN
                MERGE (%s)-[p:Derives]->(%s)
                SET p.id = randomUUID()
                SET p.popularity = r.popularity
                DELETE r
            }
            """;

    // Swap derivations of component nodes to first node
    // example: (s)-[:Derives]->(cN) -> (s)-[:Derives]->(firstN)
    queries.add(mainQuery + subquery.formatted("o:Submission", "cN", "o", "firstN"));
    queries.add(mainQuery + subquery.formatted("cN", "o:Submission", "firstN", "o"));

    // Delete all component nodes except first node
    queries.add(mainQuery + """
            CALL {
                WITH cN, firstN
                MATCH (cN)
                WHERE cN <> firstN
                DETACH DELETE cN
            }
            """);
    return queries;
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
   */
  public void runConnectedComponents(String graphName, String writeProperty) {
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
  public boolean hasProjection(String name) {
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

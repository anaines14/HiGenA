package org.higena.graph;

import org.higena.ast.TED;
import org.higena.ast.actions.TreeDiff;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ClientException;
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
  private final String challenge;
  private final String predicate;
  private Session session;
  private String name;

  public Db(String uri, String user, String password, String challenge, String predicate) {
    this(uri, user, password, "neo4j", challenge, predicate);
  }

  public Db(String uri, String user, String password, String databaseName, String challenge, String predicate) {
    this.name = databaseName;
    this.challenge = challenge;
    this.predicate = predicate;
    driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    session = driver.session(SessionConfig.forDatabase(name));
  }

  /**
   * Performs a sequence of actions to prepare the database.
   * 1. Cleans the database by deleting all nodes and edges and projections.
   * 2. Adds unique constraints to avoid duplicate IDs.
   * 3. Adds nodes to the database.
   * 4. Adds edges to the database.
   * 5. Adds the correct and incorrect labels to the nodes.
   * 6. Deletes the derivationOf, sat and cmd_n properties from the nodes.
   * 7. Aggregates nodes with the same property.
   * 8. Adds the TED to the edges.
   * 9. Adds the Poisson distribution to the edges.
   */
  public void setup() {
    deleteAllNodes();
    deleteAllProjections();
    addUniqueConstraints();
    addSubmissionNodes();
    addDerivationEdges();
    addSubmissionLabels();
    deleteProperty("derivationOf");
    deleteProperty("sat");
    deleteProperty("cmd_n");
    addEdgesPopularity();
    aggregateEquivNodes();
    addTreeDiffToEdges();
    addNodePoissonToEdges();
    //fixIncorrectOnlyPaths();
    //addPathToIncorrectLeafs();
  }

  // Algorithms

  /**
   * Runs the Dijkstra's algorithm using the given weight property to find the
   * shortest path between the source node and a Correct node.
   *
   * @param sourceId       ID of the source node.
   * @param weightProperty Property to use as weight.
   * @return Result of the dijkstra algorithm. Contains the index, source node,
   * target node, total cost, node IDs, costs and sequence of nodes in the path.
   */
  public Result dijkstra(String sourceId, String weightProperty) {
    String projectionName = "dijkstra|" + weightProperty;
    // Update projection if it exists
    if (hasProjection(projectionName)) deleteProjection(projectionName);
    addProjection(projectionName, "Submission", "Derives", weightProperty);
    // Run Dijkstra's algorithm
    return runQuery("""
            MATCH (source:Submission {id: "%s"}), (target:Correct)
            WHERE source.id <> target.id
            CALL gds.shortestPath.dijkstra.stream('%s', {
                sourceNode: source,
                targetNode: target,
                relationshipWeightProperty: '%s'
            })
            YIELD index, sourceNode, targetNode, totalCost, nodeIds, costs, path
            RETURN
                index,
                gds.util.asNode(sourceNode).id AS sourceNodeId,
                gds.util.asNode(targetNode).id AS targetNodeId,
                totalCost,
                [nodeId IN nodeIds | gds.util.asNode(nodeId).id] AS nodeIds,
                costs,
                nodes(path) AS path,
                relationships(path) AS rels
            ORDER BY totalCost
            LIMIT 1
            """.formatted(sourceId, projectionName, weightProperty));
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
    runQuery("""
            MATCH (n:Submission)
            MATCH (s:Submission)
            WHERE n.id <> s.id AND n.ast = s.ast
            MERGE (n)-[:%s]-(s)""".formatted(relName));
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
   * Adds edges between incorrect leaf nodes and the most similar correct node.
   */
  private void addPathToIncorrectLeafs() {
    // Get all incorrect leaf nodes
    Result badNodes = getIncorrectLeafs();
    // Get the most similar correct node for each incorrect node
    while (badNodes.hasNext()) {
      Node badNode = badNodes.next().get("node").asNode();
      // Get most similar correct node
      Node mostSimilarNode = getMostSimilarNode(badNode.get("ast").asString(), "Correct");
      if (mostSimilarNode != null) {
        // Create edge between the two nodes
        addEdge(badNode, mostSimilarNode);
      }
    }
    System.out.println("Added edges to incorrect leaf nodes.");
  }

  /**
   * Creates a relationship Derives between the given nodes.
   *
   * @param n1 Node 1
   * @param n2 Node 2
   * @return Relationship created.
   */
  public Relationship addEdge(Node n1, Node n2) {
    String ast1 = n1.get("ast").asString(), ast2 = n2.get("ast").asString();
    TED ted = new TED();
    TreeDiff diff = ted.computeTreeDiff(ast1, ast2);

    String query = """
            MATCH (n1:Submission {id: '%s'})
            MATCH (n2:Submission {id: '%s'})
            MERGE (n1)-[r:Derives {
              id: randomUUID(),
              ted: %d,
              operations: %s,
              popularity: 0,
              poisson: 1.5,
              dstPoisson:
              CASE
                WHEN n2.popularity = 0 THEN 1.5
                ELSE 1.0 / n2.popularity
              END
            }]->(n2)
            RETURN r AS edge""".formatted(n1.get("id").asString(), n2.get("id").asString(), diff.getTed(), diff.getActions());

    return runQuery(query).single().get(0).asRelationship();
  }

  /**
   * Creates an incorrect node in the graph with the given properties.
   *
   * @param expr Expression of the node
   * @param ast  AST of the node
   * @return The created node.
   */
  public Node addIncorrectNode(String expr, String ast, String code) {
    Result res = runQuery("""
            CREATE (n:Submission:Incorrect {id: randomUUID(),
            code: '%s',
            ast: '%s',
            expr: '%s',
            popularity: 1.0})
            RETURN n AS node""".formatted(code, ast, expr));

    return res.single().get(0).asNode();
  }

  /**
   * Adds a property to the Derives edges called dstPoisson with the value
   * 1.0 / popularity of the destination node for calculating the poisson path.
   */
  private void addNodePoissonToEdges() {
    runQuery("""
            MATCH ()-[r:Derives]->(dst:Submission)
            SET r.dstPoisson =
            CASE
              WHEN dst.popularity = 0 THEN 1.5
              ELSE 1.0/dst.popularity
            END
            """);
    System.out.println("Added node popularity to edges");
  }

  /**
   * Adds TED property to derives edges.
   */
  private void addTreeDiffToEdges() {
    // Get all edges and its nodes
    Result res = runQuery("""
            MATCH (src:Submission)-[e:Derives]->(dst:Submission)
            RETURN src.ast AS src, dst.ast AS dst, e.id AS edgeID""");
    TED ted = new TED();

    while (res.hasNext()) {
      // Get source and destination nodes of the edge + edge itself
      Record rec = res.next();
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

  /**
   * Creates a new database with the given name if it does not exist.
   *
   * @param databaseName Name of the database to create.
   */
  public void addDb(String databaseName) {
    // Create database
    runQuery("CREATE DATABASE " + databaseName + " IF NOT EXISTS");
    System.out.println("Created database " + databaseName);
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
  private void addSubmissionNodes() {
    Result res = runQuery("LOAD CSV WITH HEADERS FROM 'file:///" + this.challenge + "/" + this.predicate + ".csv' AS row\n" + """
            MERGE (s:Submission {
              id: row._id,
              cmd_n: row.cmd_n,
              code: row.code,
              derivationOf: CASE WHEN row.derivationOf IS NULL THEN '' ELSE row.derivationOf END,
              sat: toInteger(row.sat),
              expr: CASE WHEN row.expr IS NULL THEN '' ELSE row.expr END,
              ast: CASE WHEN row.ast IS NULL THEN '' ELSE row.ast END
            })
            RETURN count(s)""");

    System.out.println("Created " + res.consume().counters().nodesCreated() + " nodes.");
  }

  /**
   * Creates undirected Derives edges between nodes where the derivationOf
   * property of the source node matches the id property of the target node.
   */
  private void addDerivationEdges() {
    Result res = runQuery("""
            MATCH (s:Submission)
            MATCH (d:Submission)
            WHERE s.id = d.derivationOf AND s.id <> d.id
            MERGE (s)-[r:Derives {id: randomUUID()}]->(d)
            RETURN count(r)""");
    System.out.println("Created " + res.consume().counters().relationshipsCreated() + " Derives edges.");
  }

  private void addEdgesPopularity() {
    runQuery("""
            MATCH (n:Submission)-[r:Derives]->(s:Submission)
            CALL {
                WITH n, r, s
                MATCH (p:Submission)-[e:Derives]->(t:Submission)
                WHERE n.ast = p.ast AND s.ast = t.ast AND r.id <> e.id
                RETURN count(e) AS popularity
            }
            SET r.popularity = popularity + 1
            SET r.poisson =
            CASE
                WHEN r.popularity = 0 THEN  1.5
                ELSE 1.0 / r.popularity
            END""");

    System.out.println("Added popularity property to edges.");
  }


  /**
   * Adds Correct and Incorrect labels to nodes based on the sat property
   * (0 = Correct, 1 = Incorrect).
   */
  private void addSubmissionLabels() {
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
  private void addNodesPopularity(List<Record> components) {
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
   * @param relationship Relationship type of the edges
   */
  private void addProjection(String name, String relationship) {
    runQuery("CALL gds.graph.project('%s', '%s', '%s')".formatted(name, "Submission", relationship));
  }

  /**
   * Creates a graph projection with the given name, label, and relationship.
   * Graph projections are used to run neo4j graph data science algorithms.
   *
   * @param name         Name of the graph projection
   * @param label        Label of the nodes
   * @param relationship Relationship type of the edges
   * @param relProperty  Property of the relationship
   */
  private void addProjection(String name, String label, String relationship, String relProperty) {
    runQuery(("CALL gds.graph.project('%s', '%s', '%s', " + "{relationshipProperties: '%s'})").formatted(name, label, relationship, relProperty));
  }

  // DELETE methods

  /**
   * Delete all graph projections.
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
    Result res = runQuery("""
            MATCH (s:Submission)-[r:Derives]->(s:Submission)
            DELETE r
            RETURN count(r)""");
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

  public Result getStatistics() {
    return runQuery("""
            MATCH (s:Submission)
            WITH count(s) AS submissions
            MATCH (c:Correct)
            WITH submissions, count(c) AS corrects
            MATCH (i:Incorrect)
            WITH submissions, corrects, count(i) as incorrects
            MATCH ()-[r:Derives]->()
            WITH submissions, corrects, incorrects, count(r) AS derivations
            RETURN submissions, corrects, incorrects, derivations
            """);
  }

  /**
   * Gets all leaf nodes with an incorrect label.
   *
   * @return All nodes that have an Incorrect label.
   */
  public Result getIncorrectLeafs() {
    return runQuery("""
            MATCH (n:Incorrect)
            WHERE NOT (n)-[:Derives]->()
            RETURN n AS node
            """);
  }

  /**
   * Gets Incorrect nodes that do not have a path to a Correct node.
   *
   * @return Incorrect nodes that do not have a path to a Correct node.
   */
  private Result getIncorrectOnlyPaths() {
    return runQuery("""
            MATCH (n:Incorrect)
            WHERE NOT (n)-[:Derives*]->(:Correct)
            RETURN n AS node
            """);
  }

  /**
   * Returns the node with the given ast.
   *
   * @param ast AST of the node.
   * @return Node with the given ast.
   */
  public Node getNodeByAST(String ast) {
    Result res = runQuery("""
            MATCH (s:Submission {ast: "%s"})
            RETURN s as node
            """.formatted(ast));
    return res.hasNext() ? res.single().get("node").asNode() : null;
  }

  /**
   * Returns the most similar node to the given AST.  The
   * most similar node is different from the given AST and is
   * the node with the smaller TED. The TEd is computed using the APTED
   * algorithm. The search starts with the most popular nodes. Upon finding a
   * TED of 1, the search is stopped.
   *
   * @param ast      AST of the node to compare to the existing nodes
   * @param category Category of the nodes to compare to
   * @return Most similar node to the given AST
   */
  public Node getMostSimilarNode(String ast, String category) {

    // Get all nodes ordered by popularity
    Result res = runQuery("""
            MATCH (s:%s)
            RETURN s AS node
            ORDER BY s.popularity DESC
            """.formatted(category));

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

  public Node getMostSimilarNode(String ast) {
    return getMostSimilarNode(ast, "Submission");
  }

  /**
   * Returns the relationship between the given nodes.
   *
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
                SET p.poisson = r.poisson
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

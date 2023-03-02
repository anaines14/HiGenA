package org.example.graph;

import org.example.ast.TED;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.ArrayList;
import java.util.List;

public class Db implements AutoCloseable {
    private final Driver driver;
    private final Session session;
    private final String name;

    public Db(String uri, String user, String password, String database) {
        this.name = database;
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        session = driver.session(SessionConfig.forDatabase(database));
    }

    /**
     *
     */
    public void setup() {
        deleteAllNodes();
        addUniqueConstraints();
        addNodes("9jPK8KBWzjFmBx4Hb", "prop1");
        addEdges();
        deleteProperty("derivationOf");
        addCorrectLabel();
        addIncorrectLabel();
        aggregateEquivNodes("ast");
    }

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
     * Close the driver and session.
     *
     * @throws RuntimeException
     */
    public void close() throws RuntimeException {
        driver.close();
    }

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

    // Algorithms

    /**
     * Creates edges between nodes with the same property. Then, runs the
     * Weakly Connected Components algorithm to find the connected components
     * of the graph. Each node gets a componentId property with the id of the
     * component it belongs to.
     *
     * @param property Name of the property to aggregate by
     */
    public void aggregateEquivNodes(String property) {
        String projectionName = "equalGraph";
        String relName = "EQUAL";
        // Create edges between nodes with the same property
        runQuery("""
                MATCH (n:Submission)
                MATCH (s:Submission)
                WHERE n.id <> s.id AND n.%s = s.%s
                MERGE (n)-[:%s]-(s)""".formatted(property, property, relName));
        // Check if graph already exists
        if (!hasProjection(projectionName)) {
            // Create graph projection
            addProjection(projectionName, "Submission", relName);
        }
        // Run Weakly Connected Components algorithm
        runConnectedComponents(projectionName, "componentId");
        // Drop graph projection
        deleteProjection(projectionName);
        // Drop edges
        deleteEdges(relName);
        // Aggregate nodes: delete equivalent nodes except for one
        deleteEquivNodes();
    }

    /**
     * Runs the weakly connected components algorithm on the graph projection
     * and writes the component id of each node to the indicated property.
     *
     * @param graphName     Name of the graph projection
     * @param writeProperty Name of the property to write the component id to
     */
    public void runConnectedComponents(String graphName, String writeProperty) {
        runQuery("CALL gds.wcc.write('" + graphName + "', {writeProperty: '" +
                writeProperty + "'}) " + "YIELD " +
                "nodePropertiesWritten, componentCount");
    }

    // CREATE methods

    /**
     * Adds TED property to add derives edges.
     */
    public void addTEDToEdges() {
        Result res = getEdgesNodePair("Derives");
        TED ted = new TED();

        for (Result it = res; it.hasNext(); ) {
            Value edgeNodes = it.next().get(0);
            Node src = edgeNodes.get("src").asNode();
            Node dst = edgeNodes.get("dst").asNode();
            Relationship edge = edgeNodes.get("edge").asRelationship();

            float distance = ted.computeEditDistance(src.get("ast").asString(),
                    dst.get("ast").asString());

            runQuery("""
                    MATCH ()-[e:Derives {id: '%s'}]-()
                    SET e.ted = %f
                    """.formatted(edge.get("id").asString(), distance));
        }

    }

    /**
     * Creates a new database with the given name.
     *
     * @param name Name of the database
     */
    public void addDb(String name) {
        runQuery("CREATE DATABASE " + name + " IF NOT EXISTS");
    }

    /**
     * Adds constraint to ensure that each Submission node has a unique id
     * property and each edge Derives has a unique id property.
     */
    public void addUniqueConstraints() {
        runQuery("""
                CREATE CONSTRAINT UniqueSubmission
                IF NOT EXISTS
                FOR (s:Submission)
                REQUIRE s.id IS UNIQUE""");
        System.out.println("Created unique node.id constraint.");

        runQuery("""
                CREATE CONSTRAINT UniqueDerives
                IF NOT EXISTS
                FOR (d:Derives)
                REQUIRE d.id IS UNIQUE""");
        System.out.println("Created unique edge.id constraint.");
    }

    /**
     * Loads nodes from a csv file with Alloy4Fun submissions into the database.
     *
     * @param challenge Challenge/Folder name (e.g. 9jPK8KBWzjFmBx4Hb)
     * @param pred      Predicate name/File name (e.g. prop1) without extension
     */
    public void addNodes(String challenge, String pred) {
        Result res = runQuery("LOAD CSV WITH HEADERS FROM 'file:///" + challenge +
                "/" + pred + ".csv' AS row\n" +
                "MERGE (s:Submission {\n" +
                "\tid: row._id,\n" +
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

    /**
     * Creates undirected Derives edges between nodes where the derivationOf
     * property of the source node matches the id property of the target node.
     */
    public void addEdges() {
        Result res = runQuery("""
                MATCH (s:Submission)
                MATCH (d:Submission)
                WHERE s.id = d.derivationOf AND s.id <> d.id
                MERGE (s)-[r:Derives {id: randomUUID()}]-(d)
                RETURN count(r)""");
        System.out.println("Created " + res.consume().counters().relationshipsCreated() +
                " Derives edges.");
    }

    /**
     * Adds a Correct label to all nodes with a sat property of 1.
     */
    public void addCorrectLabel() {
        Result res = runQuery("""
                MATCH (s:Submission {sat: 1})
                SET s:Correct
                RETURN count(s)""");
        System.out.println("Set " + res.consume().counters().labelsAdded() + " " +
                "Correct labels.");
    }

    /**
     * Adds an Incorrect label to all nodes with a sat property of 0.
     */
    public void addIncorrectLabel() {
        Result res = runQuery("""
                MATCH (s:Submission {sat: 0})
                SET s:Incorrect
                RETURN count(s)""");
        System.out.println("Set " + res.consume().counters().labelsAdded() + " " +
                "Incorrect labels.");
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
        runQuery("CALL gds.graph.project('%s', '%s', '%s')"
                .formatted(name, label, relationship));
    }

    // DELETE methods

    /**
     * Deletes all nodes and edges from the database.
     */
    public void deleteAllNodes() {
        runQuery("MATCH (n) DETACH DELETE n");
        System.out.println("Delete all nodes.");
    }

    /**
     * Deletes property from all nodes.
     *
     * @param property Name of the property to delete
     */
    public void deleteProperty(String property) {
        Result res = runQuery("MATCH (s:Submission)\n" +
                "REMOVE s." + property + "\n" +
                "RETURN count(s)");
        System.out.println("Removed " + res.consume().counters().propertiesSet() +
                " " + property + " properties.");
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
     * Deºetes all edges of the given relationship type.
     *
     * @param relationship Relationship type of the edges to delete
     */
    public void deleteEdges(String relationship) {
        Result res = runQuery("MATCH ()-[r:" + relationship + "]-()\n" +
                "DELETE r\n" +
                "RETURN count(r)");
        System.out.println("Deleted " + res.consume().counters().relationshipsDeleted() + " edges.");
    }

    /**
     * Deletes the database with the given name.
     *
     * @param name Name of the database to delete
     */
    public void deleteDb(String name) {
        runQuery("DROP DATABASE " + name + " IF EXISTS");
    }

    /**
     * Deletes all loops of the given relationship type. Loops are edges
     * where the source and target node are the same.
     *
     * @param relationship Relationship type of the loops to delete
     */
    public void deleteLoops(String relationship) {
        Result res = runQuery("MATCH (s:Submission)-[r:" + relationship + "]->" +
                "(s:Submission)\n" +
                "DELETE r\n" +
                "RETURN count(r)");
        System.out.println("Deleted " + res.consume().counters().relationshipsDeleted() + " loops.");
    }

    /**
     * Deletes equivalent nodes. Equivalent nodes are nodes that belong to the
     * same component after running the connected components algorithm.
     * For each component the first node is kept and all other nodes are deleted.
     * Derivations of deleted nodes are updated to point to the first node of the
     * component.
     */
    public void deleteEquivNodes() {
        Result rest = getDistinctPropertyValues("componentId");
        // Iterate over all components
        while (rest.hasNext()) {
            int componentId = rest.next().get("componentId").asInt();
            // Remove equivalent nodes from component
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
     * Gets edges with the given relationship type and returns them in a list
     * with the source node, the edge, and the target node. Example: [src,edge,dst]
     *
     * @param relation Relationship type of the edges
     * @return Result object containing lists of the source node, the edge, and the target node
     */
    public Result getEdgesNodePair(String relation) {
        return runQuery("""
                MATCH (s:Submission)-[e:%s]-(d:Submission)
                RETURN {src: s, edge: e, dst: d}
                """.formatted(relation));
    }

    /**
     * Returns all nodes with the given property value.
     *
     * @param property Name of the property
     * @param value    Value of the property
     * @return Result object containing all nodes with the given property value
     */
    public Result getNodesWithPropertyValue(String property, int value) {
        return runQuery("MATCH (s:Submission)\n" +
                "WHERE s." + property + " = " + value + " RETURN s");
    }

    /**
     * Returns all distinct values of the given property.
     *
     * @param property Name of the property
     * @return Result object containing all distinct values of the given property
     */
    public Result getDistinctPropertyValues(String property) {
        return runQuery("MATCH (s:Submission)\n" +
                "RETURN DISTINCT s." + property + " AS " + property);
    }

    /**
     * Returns the name of the database.
     *
     * @return Name of the database
     */
    public String getName() {
        return name;
    }

    /**
     * Auxiliary method to create queries for deleting equivalent nodes.
     *
     * @param componentId Id of the component
     * @return List of queries
     */
    private List<String> getDelEquivNodesQueries(int componentId) {
        List<String> queries = new ArrayList<>();
        // Get all nodes in component and the first node of the component
        String mainQuery = """
                MATCH (n:Submission {componentId: %d})
                WITH collect(DISTINCT n) AS compNodes
                WITH compNodes, compNodes[0] AS firstN
                UNWIND compNodes as cN
                """.formatted(componentId);
        // Swap derivations of component nodes to first node
        // example: (s)-[:Derives]->(cN) -> (s)-[:Derives]->(firstN)
        queries.add(mainQuery + """
                CALL {
                    WITH cN, firstN
                    MATCH (s:Submission)-[r:Derives]-(cN)
                    WHERE cN <> firstN
                    MERGE (s)-[:Derives]-(firstN)
                    DELETE r
                }
                """
        );
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

}

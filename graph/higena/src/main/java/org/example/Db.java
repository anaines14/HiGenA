package org.example;

import org.neo4j.driver.*;

public class Db implements AutoCloseable {
    private final Driver driver;
    private final Session session;

    public Db(String uri, String user, String password, String database) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        session = driver.session(SessionConfig.forDatabase(database));
    }

    public void setup() {
        createUniqueConstraint();
        System.out.println("Created unique constraint");

        Result res = loadNodes("9jPK8KBWzjFmBx4Hb", "prop1");
        System.out.println("Created " + res.consume().counters().nodesCreated() + " nodes");

        res = setEdges();
        System.out.println("Created " + res.consume().counters().relationshipsCreated() + " edges");

        res = setCorrectLabel();
        System.out.println("Set " + res.consume().counters().labelsAdded() + " Correct labels");

        res = setIncorrectLabel();
        System.out.println("Set " + res.consume().counters().labelsAdded() + " Incorrect labels");
    }

    public void createDb(String name) {
        runQuery("CREATE DATABASE " + name + " IF NOT EXISTS");
    }

    public void dropDb(String name) {
        runQuery("DROP DATABASE " + name + " IF EXISTS");
    }

    public void createUniqueConstraint() {
        runQuery("CREATE CONSTRAINT UniqueSubmission IF NOT EXISTS\n" +
                "FOR (s:Submission)\n" +
                "REQUIRE s._id IS UNIQUE");
    }

    public Result loadNodes(String challenge, String pred) {
        return runQuery("LOAD CSV WITH HEADERS FROM 'file:///" + challenge + "/" + pred + ".csv' AS row\n" +
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
    }

    public Result setEdges() {
        return runQuery("MATCH (s:Submission)\n" +
                "MATCH (d:Submission)\n" +
                "WHERE s._id = d.derivationOf AND s._id <> d._id\n" +
                "MERGE (s)-[r:Derives]->(d)\n" +
                "RETURN count(r)");
    }

    public Result setCorrectLabel() {
        return runQuery("MATCH (s:Submission {sat: 1})\n" +
                "SET s:Correct\n" +
                "RETURN count(s)");
    }

    public Result setIncorrectLabel() {
        return runQuery("MATCH (s:Submission {sat: 0})\n" +
                "SET s:Incorrect\n" +
                "RETURN count(s)");
    }

    public void verifyConnection() {
        driver.verifyConnectivity();
    }

    public Result runQuery(String query) {
        return session.run(new Query(query));
    }

    public void close() throws RuntimeException {
        driver.close();
    }
}

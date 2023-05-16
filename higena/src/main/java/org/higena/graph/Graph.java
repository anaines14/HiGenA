package org.higena.graph;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.higena.ast.Parser;
import org.higena.graph.hint.Hint;
import org.higena.graph.hint.HintGenType;
import org.higena.graph.hint.HintGenerator;
import org.neo4j.driver.Record;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Wrapper class for the database (Db class). It provides methods to set up the
 * database and to generate hints.
 */
public class Graph {
  private final String uri, user, password, databaseName, challenge, predicate;
  private Parser parser = null;

  public Graph(String challenge, String predicate, String filename) {
    this(challenge, predicate);
    this.parser = Parser.fromFile(filename);
  }

  public Graph(String challenge, String predicate) {
    String uri, user, password;
    try {
      // Use .env
      Dotenv dotenv = Dotenv.load();

      uri = dotenv.get("URI_NEO4J");
      user = dotenv.get("USERNAME_NEO4J");
      password = dotenv.get("PASSWORD_NEO4J");
    } catch (DotenvException e) {
      // no .env file found
      uri = System.getenv("URI_NEO4J");
      user = System.getenv("USERNAME_NEO4J");
      password = System.getenv("PASSWORD_NEO4J");
    }

    if (uri == null || user == null || password == null) {
      System.out.println("Please set the environment variables URI_NEO4J, " +
              "USERNAME_NEO4J and PASSWORD_NEO4J");
      System.exit(1);
    }

    this.uri = uri;
    this.user = user;
    this.password = password;

    this.challenge = challenge;
    this.predicate = predicate;
    this.databaseName = genDatabaseName(challenge, predicate);

    // Connect to the default database
    try (Db db = new Db(uri, user, password, challenge, predicate)) {
      db.verifyConnection();
      // Create database for this challenge and predicate if it does not exist
      db.addDb(this.databaseName);
    }
  }

  /**
   * Generates a databaseName accepted by neo4j. It removes all digits from the
   * challenge name, converts it to lowercase and takes the first 4 characters.
   * Then, if the predicate contains digits, it transforms them into letters using
   * the remainder of the division by 26 of the number (e.g 0 -> a, 1 -> b, etc.)
   * Otherwise, it just appends the predicate.
   *
   * @param challenge Challenge name
   * @param predicate Predicate name
   * @return Database name
   */
  private String genDatabaseName(String challenge, String predicate) {
    // remove all digits, convert to lowercase and take the first 4 characters
    StringBuilder ret = new StringBuilder(challenge.replaceAll("\\d", "").toLowerCase().substring(0, 4));

    // If predicate contains digits, transform them into letters
    if (predicate.matches(".*\\d.*")) {
      String[] numbers = predicate.replaceAll("[^0-9]", "").split("");
      for (Iterator<String> it = Arrays.stream(numbers).iterator(); it.hasNext(); ) {
        String number = it.next();
        ret.append((char) (Integer.parseInt(number) + 97));
      }
    } else {
      // If predicate does not contain digits, just append it
      ret.append(predicate);
    }
    return ret.toString();
  }

  // Hint methods

  /**
   * Sets up the graph database.
   */
  public void setup() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      System.out.println("[SETUP] Database: " + databaseName);
      long startTime = System.currentTimeMillis();
      try {
        db.setup();
      } catch (Exception e) {
        System.err.println("FAILED SETUP: " + e.getMessage());
        return;
      }
      long endTime = System.currentTimeMillis() - startTime;
      System.out.println("Finished setup in " + endTime + " ms.");
    }
  }

  /**
   * Returns a hint for the given expression. The hint is generated using the
   * given type of generation.
   *
   * @param expr Expression to generate the hint for.
   * @param type Type of hint generation.
   * @return Hint for the given expression.
   */
  public Hint getHint(String expr, HintGenType type) {
    return getHint(expr, "", type);
  }

  /**
   * Returns a hint for the given expression and code. The hint is generated
   * using the given type of generation.
   *
   * @param expr Expression to generate the hint for.
   * @param code Alloy code used by the expression.
   * @param type Type of hint generation.
   * @return Hint for the given expression.
   */
  public Hint getHint(String expr, String code, HintGenType type) {
    HintGenerator generator = generateHint(expr, code, type);
    if (generator == null) {
      return null;
    }
    return generator.getHint();
  }

  /**
   * Generates a hint for the given expression. The hint is generated using the
   * given type of generation.
   *
   * @param expr Expression to generate the hint for.
   * @param type Type of hint generation.
   * @return Hint Generator object that generated the hint.
   */
  public HintGenerator generateHint(String expr, String code,
                                    HintGenType type) {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      if (parser == null) { // if no parser is set, use the original code
        try {
          setParser();
        } catch (Exception e) {
          System.err.println("ERROR: Missing empty submission on the graph.");
          return null;
        }
      }
      String ast = parser.parse(expr, code);
      if (ast == null) {
        return null;
      }
      HintGenerator generator = new HintGenerator(expr, code, type, db);
      generator.generateHint(ast);
      System.out.println(generator);
      return generator;
    }
  }

  /**
   * Returns statistics for the current database (number of nodes, edges,
   * correct nodes, incorrect nodes).
   *
   * @return Record with the statistics.
   */
  public Record getStatistics() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      return db.getStatistics().single();
    }
  }

  /**
   *
   */
  private void setParser() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      String model = db.getOriginalCode();
      parser = Parser.fromModel(model);
    }
  }

}

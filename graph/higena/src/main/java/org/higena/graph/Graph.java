package org.higena.graph;

import io.github.cdimascio.dotenv.Dotenv;
import org.higena.ast.Parser;
import org.higena.graph.hint.Hint;
import org.higena.graph.hint.HintGenType;
import org.higena.graph.hint.HintGenerator;
import org.neo4j.driver.Record;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Wrapper class for the database (Db class).
 */
public class Graph {
  private final String uri, user, password, databaseName, challenge, predicate;
  private final Parser parser;

  public Graph(String challenge, String predicate) {
    Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();

    this.uri = dotenv.get("NEO4J_URI");
    this.user = dotenv.get("NEO4J_USERNAME");
    this.password = dotenv.get("NEO4J_PASSWORD");
    this.challenge = challenge;
    this.predicate = predicate;
    this.databaseName = genDatabaseName(challenge, predicate);
    this.parser = new Parser(challenge);

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

  /**
   * Sets up the graph database.
   */
  public void setup() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      System.out.println("Starting DB setup...");
      long startTime = System.currentTimeMillis();
      db.setup();
      long endTime = System.currentTimeMillis() - startTime;
      System.out.println("Success: Finished setup in " + endTime + " ms.");
    }
  }

  // Hint methods

  /**
   * Returns a hint for the given expression. The hint is generated using the
   * given type of generation.
   * @param expr Expression to generate the hint for.
   * @param type Type of hint generation.
   * @return Hint for the given expression.
   */
  public Hint getHint(String expr, HintGenType type) {
    return getHint(expr, "", type);
  }

  /**
   * Returns a hint for the given expression. The hint is generated using the
   * given type of generation.
   * @param expr Expression to generate the hint for.
   * @param code Code used by expression.
   * @param type Type of hint generation.
   * @return Hint for the given expression.
   */
  public Hint getHint(String expr, String code, HintGenType type) {
    return generateHint(expr, code, type).getHint();
  }

  public HintGenerator generateHint(String expr, String code,
                                    HintGenType type) {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      String ast = parser.parse(expr, code);
      HintGenerator generator = new HintGenerator(expr, code, type, db);
      generator.generateHint(ast);
      System.out.println("\n---------------------");
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

}

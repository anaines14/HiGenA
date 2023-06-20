package org.higena.graph;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.higena.hint.Hint;
import org.higena.hint.HintGenType;
import org.higena.hint.HintGenerator;
import org.higena.parser.A4FParser;
import org.neo4j.driver.Record;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper class for the database (Db class). It provides methods to set up the
 * database and to generate hints.
 */
public class Graph {
  private final String uri, user, password, databaseName, challenge, predicate;
  private CompModule challengeModule;

  public Graph(String challenge, String predicate, String filename) {
    this(challenge, predicate);
    this.challengeModule = CompUtil.parseEverything_fromFile(new A4Reporter(), null,
            filename);
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
      if (challengeModule == null) { // if no challengeModule is set, use the
        // original code
        try {
          setChallengeModule();
        } catch (Exception e) {
          System.err.println("ERROR: Missing empty submission on the graph.");
          return null;
        }
      }
      String ast = parse(expr, code);
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
   * Runs a query on the graph database.
   * @param query Query to run.
   * @return List of records returned by the query.
   */
  public List<Record> runQuery(String query) {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      return db.runQuery(query).list();
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
   * Sets the challenge module to the original code of the challenge. Fetches
   * the original code from the empty submission on the graph.
   */
  private void setChallengeModule() {
    try (Db db = new Db(uri, user, password, databaseName, challenge, predicate)) {
      String model = db.getOriginalCode();
      challengeModule = CompUtil.parseEverything_fromString(new A4Reporter(),
              model);
    }
  }

  // Parse functions

  /**
   * Parses an Alloy expression using the challenge module and returns the AST
   * of the parsed expression.
   *
   * @param expression The expression to parse.
   * @return The AST of the parsed expression.
   */
  private String parse(String expression) {
    if (expression.equals("")) {
      return "";
    }
    return A4FParser.parse(expression, this.challengeModule).toTreeString();
  }

  /**
   * Parses an Alloy expression using the full module code and returns the AST of the parsed expression.
   *
   * @param expression The expression to parse.
   * @param code       The full module code.
   * @return The AST of the parsed expression.
   */
  public String parse(String expression, String code) {
    String ast;
    try {
      ast = parse(expression);
    } catch (Exception parseExprException) {
      try {
        ast = A4FParser.parse(expression, code).toTreeString();
      } catch (Exception parseException) {
        System.err.println("Error parsing expression: " + expression);
        return null;
      }
    }
    return ast;
  }

}

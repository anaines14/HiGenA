import org.higena.ast.TED;
import org.higena.graph.Graph;
import org.higena.hint.HintGenType;
import org.higena.hint.HintGenerator;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class HintTest {

  private static final String CHALLENGES_DIR = "../data/datasets/challenges/";
  private static final String PATH = "src/test/outputs/";
  private static final boolean statistics = true;

  // Tests

  private static Stream<Arguments> hintInputsProvider() {
    return Stream.of(
            // TreeInsert
            Arguments.of("zRAn69AocpkmxXZnW", "inv7", "all c : Class | some Teaches.c"));
  }

  /**
   * Generates a hint for the given challenge, predicate and expression.
   * @param challenge Challenge name
   * @param predicate Predicate name
   * @param expr Expression to generate hint for
   */
  @ParameterizedTest
  @MethodSource("hintInputsProvider")
  public void singleHintTest(String challenge, String predicate, String expr) {
    genSingleHint(challenge, predicate, expr);
  }

    /**
     * Generates hint for the given challenge, predicate and expression and appends hint into a csv file.
     * @param challenge Challenge name
     * @param predicate Predicate name
     */
  @ParameterizedTest
  @CsvFileSource(resources = "/most_popular.csv", numLinesToSkip = 1, delimiter = ';')
  public void fileHintTest(String challenge, String predicate, String expr) {
    File file = new File(PATH + "teacher_study.csv");

    HintGenerator hintGen = genSingleHint(challenge, predicate, expr);
    String hint = hintGen.getHint().toString(), solution = hintGen.getJSON().getString("targetExpr");

    try {
      FileWriter writer = new FileWriter(file, true);
      writer.write("\n" + challenge + ";" + predicate + ";" + expr + ";" + solution + ";" + hint);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Tests hint generation using APTED, creating new paths and using the TED
   * for the cost function. Generated hints are written to a log file. Uses
   * the train data to create a graph and test data to ask for hints.
   */
  @Test
  public void testAllHintGen() {
    // Create log file
    File logFile = createLogFile("hint_stats");

    testDataProvider().forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, HintGenType.TED, logFile);

    });
  }

  public HintGenerator genSingleHint(String challenge, String predicate, String expr) {
    Graph graph = new Graph(challenge, predicate);
    graph.setup();
    return graph.generateHint(expr, "", HintGenType.TED);
  }

  /**
   * Generates hints for a given challenge, predicate and test data. The test
   * data is present in a file, where each line is a JSON object containing the
   * expression and code. The generated hints are written to a log file.
   * Before generating each hint, the graph is reset by calling the setup
   * method so that each hint does not interfere with the other.
   * @param challenge Name of the challenge
   * @param predicate Name of the predicate
   * @param test_data File containing the test data in JSON format
   * @param genType Type of hint generation to use (TED, NODE_POISSON, REL_POISSON)
   * @param logFile File to write the generated hints to
   */
  private void genHints(String challenge, String predicate, File test_data,
                        HintGenType genType, File logFile) {

    String filename = CHALLENGES_DIR + challenge + ".als";
    // Load train data
    Graph g = new Graph(challenge, predicate, filename);

    // Load test data
    List<Map.Entry<String, String>> expressions = getTestSubmissions(test_data);

    // Generate hints
    for (Map.Entry<String, String> entry : expressions) {
      // Reset graph for each submission
      System.out.println("\n");
      g.setup();

      // Generate hint
      String expr = entry.getKey(), code = entry.getValue();
      System.out.println("\n[HINT]: " + expr);
      HintGenerator hintGen = g.generateHint(expr, code, genType);

      // Log hint
      if (statistics)
        writeStatistics(logFile, hintGen, challenge, predicate);
    }
  }

  // data providers

  /**
   * Provides the test data files for each challenge and predicate for the
   * hint generation tests. The test data files are present in the data/test
   * directory. Each challenge is a directory containing the test
   * data for each predicate in JSON files that contain the test data.
   * @return Stream of arguments for each test
   */
  private static Stream<Arguments> testDataProvider() {
    String test_dir = "../data/datasets/prepared/test";
    Stream<Arguments> stream = Stream.of();

    // Iterate challenges and predicates
    for (File challengeDir : Objects.requireNonNull(new File(test_dir).listFiles())) {
      for (File predicateFile : Objects.requireNonNull(challengeDir.listFiles())) {
        if (predicateFile.length() == 0) continue; // Skip empty files
        // Get challenge and predicate names
        String challenge = challengeDir.getName();
        String predicate = predicateFile .getName().replace(".json", "");
        // Append arguments to stream (challenge, predicate, predicate file
        // with test data)
        stream = Stream.concat(stream, Stream.of(Arguments.of(challenge, predicate, predicateFile)));
      }
    }
    return stream;
  }

  /**
   * Returns the test submissions for a given test data file. The test data
   * file is a JSON file containing the test submissions. Each line is a JSON
   * object containing the expression and code and the sat value indicating
   * whether the expression is correct or not. The incorrect test submissions
   * are returned as a list of entries, where each entry is a pair of expression
   * and code.
   * @param dataFile File containing the test data
   * @return List of incorrect test submissions as a list of entries
   * (expression, code)
   */
  private List<Map.Entry<String, String>> getTestSubmissions(File dataFile) {
    List<Map.Entry<String, String>> submissions = new ArrayList<>();

    // Parse JSON file
    try {
      Scanner scanner = new Scanner(dataFile);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();

        // Parse object
        JSONObject obj = new JSONObject(line);
        int sat = obj.getInt("sat");
        if (sat == 0) continue; // Skip correct expressions
        String expr = obj.getString("expr"), code = obj.getString("code");
        submissions.add(new AbstractMap.SimpleEntry<>(expr, code));
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    return submissions;
  }

  // Logging methods

  /**
   * Writes a line to a file.
   * @param file File to write to
   * @param line Line to write
   */
  public static void writeLineToFile(File file, String line) {
    try {
      FileWriter writer = new FileWriter(file, true);
      writer.write(line + '\n');
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes statistics of a hint generation to a log file. The statistics
   * come from the hint generator JSON object representation. The challenge
   * and predicate names are also written to the log file.
   * @param logFile File to write to
   * @param hintGen Hint generator
   * @param challenge Challenge name
   * @param predicate Predicate name
   */
  public static void writeStatistics(File logFile, HintGenerator hintGen,
                                     String challenge, String predicate) {
    if (hintGen == null) return;
    JSONObject obj = hintGen.getJSON();
    obj.put("challenge", challenge);
    obj.put("predicate", predicate);
    String row = obj.toString();
    writeLineToFile(logFile, row);
  }

  /**
   * Creates a log file with the given name in the src/test/outputs directory.
   * If a file with the same name already exists, it is deleted.
   * @param name Name of the log file
   * @return The created log file
   */
  public File createLogFile(String name) {
    if (!statistics) return null;
    File file = new File(PATH + name + ".json");
    file.delete(); // Delete file if it already exists
    return file;
  }

}

import org.higena.ast.TED;
import org.higena.graph.Graph;
import org.higena.graph.hint.HintGenType;
import org.higena.graph.hint.HintGenerator;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class HintTest {

  // Tests

  /**
   * Tests hint generation using APTED, without creating new paths and using the
   * TED for the cost function. Generated hints are written to a log file. Uses
   * the train data to create a graph and test data to ask for hints.
   */
  @Test
  public void testNoNewPathsHintGen() {
    // Create log file
    File logFile = createLogFile("no_new_paths_hint_stats");

    // Set hint generation settings
    HintGenerator.cantCreatePath = true;
    TED.USE_APTED = true;

    testDataProvider().forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, HintGenType.TED, logFile);
    });
  }

  /**
   * Tests hint generation using APTED, creating new paths and using the TED
   * for the cost function. Generated hints are written to a log file. Uses
   * the train data to create a graph and test data to ask for hints.
   */
  @Test
  public void testAptedHintGen() {
    // Create log file
    File logFile = createLogFile("apted_hint_stats");

    // Set hint generation settings
    HintGenerator.cantCreatePath = false;
    TED.USE_APTED = true;

    testDataProvider().forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, HintGenType.TED, logFile);

    });
  }


  /**
   * Tests hint generation using GumTree, creating new paths and using the TED
   * for the cost function. Generated hints are written to a log file. Uses
   * the train data to create a graph and test data to ask for hints.
   */
  @Test
  public void testGumTreeHintGen() {
    // Create log file
    File logFile = createLogFile("gumtree_hint_stats");

    // Set hint generation settings
    HintGenerator.cantCreatePath = false;
    TED.USE_APTED = false;

    testDataProvider().forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, HintGenType.TED, logFile);
    });
  }

  /**
   * Tests hint generation using APTED, creating new paths and using the node
   * popularity poisson distribution for the cost function. Generated hints are
   * written to a log file. Uses the train data to create a graph and test data
   * to ask for hints.
   */
  @Test
  public void testNodePopularityHintGen() {
    // Create log file
    File logFile = createLogFile("node_poisson_hint_stats");

    // Set hint generation settings
    HintGenerator.cantCreatePath = false;
    TED.USE_APTED = true;

    testDataProvider().forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, HintGenType.NODE_POISSON, logFile);
    });
  }

  /**
   * Tests hint generation using APTED, creating new paths and using the edge
   * popularity poisson distribution for the cost function. Generated hints are
   * written to a log file. Uses the train data to create a graph and test data
   * to ask for hints.
   */
  @Test
  public void testEdgePopularityHintGen() {
    // Create log file
    File logFile = createLogFile("edge_poisson_hint_stats");

    // Set hint generation settings
    HintGenerator.cantCreatePath = false;
    TED.USE_APTED = true;

    testDataProvider().forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, HintGenType.REL_POISSON, logFile);
    });

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

    // Load train data
    Graph g = new Graph(challenge, predicate);

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
      HintGenerator hintGen = g.generateHint(expr, code, HintGenType.TED);

      // Log hint
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
    String test_dir = "../data/test";
    Stream<Arguments> stream = Stream.of();

    // Iterate challenges and predicates
    for (File challengeDir : Objects.requireNonNull(new File(test_dir).listFiles())) {
      for (File predicateFile : Objects.requireNonNull(challengeDir.listFiles())) {
        if (predicateFile .length() == 0) continue; // Skip empty files
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
    String PATH = "src/test/outputs/";
    File file = new File(PATH + name + ".json");
    file.delete(); // Delete file if it already exists
    return file;
  }

}

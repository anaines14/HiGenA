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
  private static final Stream<Arguments> test_data = testDataProvider();

  // Tests

  @Test
  public void testNoNewPathsHintGen() {
    // Create log file
    File logFile = createLogFile("no_new_paths_hint_stats");

    // Set hint generation settings
    HintGenerator.cantCreatePath = true;

    test_data.forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, logFile);
    });
  }

  @Test
  public void testAptedHintGen() {
    // Create log file
    File logFile = createLogFile("apted_hint_stats");

    // Set hint generation settings
    HintGenerator.cantCreatePath = false;
    TED.USE_APTED = true;

    test_data.forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, logFile);

    });
  }

  @Test
  public void testGumTreeHintGen() {
    // Create log file
    File logFile = createLogFile("gumtree_hint_stats");

    // Set hint generation settings
    HintGenerator.cantCreatePath = false;
    TED.USE_APTED = false;

    test_data.forEach(args -> {
      String challenge = (String) args.get()[0], predicate = (String) args.get()[1];
      File test_data = (File) args.get()[2];
      genHints(challenge, predicate, test_data, logFile);
    });
  }

  @Test
  public void testNodePopularityHintGen() {

  }

  @Test
  public void testEdgePopularityHintGen() {

  }

  private void genHints(String challenge, String predicate, File test_data,
                       File logFile) {
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
      System.out.println("\n[HINT]");
      HintGenerator hintGen = g.generateHint(expr, code, HintGenType.TED);

      // Log hint
      writeStatistics(logFile, hintGen, challenge, predicate);
    }
  }

  // data providers

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

  public static void writeLineToFile(File file, String line) {
    try {
      FileWriter writer = new FileWriter(file, true);
      writer.write(line + '\n');
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeStatistics(File logFile, HintGenerator hintGen,
                                     String challenge, String predicate) {
    JSONObject obj = hintGen.getJSON();
    obj.put("challenge", challenge);
    obj.put("predicate", predicate);
    String row = obj.toString();
    writeLineToFile(logFile, row);
  }

  public File createLogFile(String name) {
    String PATH = "src/test/outputs/";
    File file = new File(PATH + name + ".json");
    file.delete(); // Delete file if it already exists
    return file;
  }

}

import org.higena.graph.Graph;
import org.higena.graph.hint.HintGenType;
import org.higena.graph.hint.HintGenerator;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class HintTest {
  private static File file = null;
  private static final boolean statistics = true;

  @BeforeAll
  public static void setup() {
    if (statistics)
      createFile("hints_statistics", "src/test/outputs/");
  }

  // Test method

  private static Stream<Arguments> testDataProvider() {
    String test_dir = "../data/test";
    Stream<Arguments> stream = Stream.of();

    // Iterate challenges and predicates
    for (File challenge_dir : Objects.requireNonNull(new File(test_dir).listFiles())) {
      for (File predicate_file : Objects.requireNonNull(challenge_dir.listFiles())) {
        if (predicate_file.length() == 0) continue; // Skip empty files
        // Get challenge and predicate names
        String challenge = challenge_dir.getName();
        String predicate = predicate_file.getName().replace(".json", "");
        // Append arguments to stream (challenge, predicate, predicate file
        // with test data)
        stream = Stream.concat(stream, Stream.of(Arguments.of(challenge,
                predicate, predicate_file)));
      }
    }
    return stream;
  }

  public static void createFile(String name, String path) {
    file = new File(path + name + ".json");
    file.delete();
  }

  // Auxiliary methods

  public static void writeLineToFile(String line) {
    try {
      FileWriter writer = new FileWriter(file, true);
      writer.write(line + '\n');
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // Logging methods

  public static void writeStatistics(HintGenerator hintGen, String challenge,
                                     String predicate) {
    JSONObject obj = hintGen.getJSON();
    obj.put("challenge", challenge);
    obj.put("predicate", predicate);
    String row = obj.toString();
    writeLineToFile(row);
  }

  @MethodSource("testDataProvider")
  @ParameterizedTest
  public void testHintGen(String challenge, String predicate, File test_data) {
    // Load train data
    Graph g = new Graph(challenge, predicate);
    // Load test data
    List<Map.Entry<String, String>> expressions = getTestData(test_data);
    // Generate hints
    for (Map.Entry<String, String> entry : expressions) {
      g.setup(); // Reset graph
      String expr = entry.getKey(), code = entry.getValue();
      // Generate hint
      HintGenerator hintGen = g.generateHint(expr, code, HintGenType.TED);
      // Log hint if analytics enabled
      if (statistics)
        writeStatistics(hintGen, challenge, predicate);
    }
  }

  private List<Map.Entry<String, String>> getTestData(File data_file) {
    List<Map.Entry<String, String>> submissions = new ArrayList<>();

    // Parse JSON file
    try {
      Scanner scanner = new Scanner(data_file);
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
}

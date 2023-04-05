import org.higena.graph.Graph;
import org.higena.graph.Hint;
import org.higena.graph.HintGenType;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Stream;

public class HintTest {
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
        //break
      }
      //break;
    }
    return stream;
  }

  @MethodSource("testDataProvider")
  @ParameterizedTest
  public void testHintGen(String challenge, String predicate, File test_data) {
    // Load train data
    Graph g = new Graph(challenge, predicate);
    g.setup();
    System.out.println("\n");
    // Load test data
    List<String> expressions = getTestData(test_data);
    // Generate hints
    for (String expr : expressions) {
      System.out.println("----------------------------------------\n");
      Hint hint = g.getHint(expr, HintGenType.TED);
      System.out.println("HINT:\n" + hint.toHintMsg() + "\n");
    }
  }

  private List<String> getTestData(File data_file) {
    List<String> expressions = new ArrayList<>();

    // Parse JSON file
    try {
      Scanner scanner = new Scanner(data_file);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        JSONObject obj = new JSONObject(line);
        int sat = obj.getInt("sat");
        if (sat == 0) continue; // Skip correct expressions
        String expr = obj.getString("expr");

        System.out.println("ID: " + obj.getString("_id") + ", SAT: " + sat +
                ", EXPR: " + expr);
        expressions.add(expr);
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    return expressions;
  }
}

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import org.higena.graph.Graph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.Record;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DBSetupTest {

  private static final String CHALLENGES_DIR = "../data/datasets/challenges/";
  private static File csv = null;
  private static final boolean statistics = true;

  @BeforeAll
  public static void setup() {
    createCSV("graph_statistics", "src/test/outputs/");
  }

  // Test method

  @ParameterizedTest
  @MethodSource("getDatasets")
  public void testDBSetup(String challenge, String predicate) {
    String filename = CHALLENGES_DIR + challenge + ".als";
    Graph g = new Graph(challenge, predicate, filename);
    g.setup();

    if (statistics) {
      writeStatistics(g.getStatistics(), challenge, predicate);
    }
  }

  public static Stream<Arguments> getDatasets() {
    List<String> challenges = Arrays.stream(Objects.requireNonNull(new File(CHALLENGES_DIR).list())).toList();
    HashMap<String, List<String>> arguments = new HashMap<>();

    for (String challenge : challenges) {
      CompModule module = CompUtil.parseEverything_fromFile(new A4Reporter(), null, CHALLENGES_DIR+ challenge);
      List<String> predicates = new java.util.ArrayList<>(module.getAllFunc().makeConstList().stream().map(c -> c.label).toList());
      predicates.remove(predicates.size() - 1);

      arguments.put(challenge, predicates);
    }

    return arguments.entrySet().stream().flatMap(e -> e.getValue().stream().map(p -> Arguments.of(e.getKey().replace(".als", ""), p.replace("this/", ""))));
  }

  // Logging methods

  public static void createCSV(String name, String path) {
    String columns = "Challenge,Predicate,NumSubmissions,NumCorrect," + "NumIncorrect,NumEdges";
    csv = new File(path + name + ".csv");
    csv.delete();

    try {
      if (csv.createNewFile()) { // create new file
        // Write columns to file
        writeLineToCSV(columns);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeLineToCSV(String line) {
    try {
      FileWriter writer = new FileWriter(csv, true);
      writer.write(line + '\n');
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeStatistics(Record record, String challenge, String predicate) {
    String row = challenge + "," + predicate + "," + record.get("submissions").asInt() + "," + record.get("corrects").asInt() + "," + record.get("incorrects").asInt() + "," + record.get("derivations").asInt();
    writeLineToCSV(row);
  }
}

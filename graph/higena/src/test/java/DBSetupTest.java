import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import org.higena.graph.Graph;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DBSetupTest {
  public static Stream<Arguments> getDatasets() {
    String dir = "src/main/resources/challenges";
    List<String> challenges = Arrays.stream(Objects.requireNonNull(new File(dir).list())).toList();
    HashMap<String, List<String>> arguments = new HashMap<>();

    for (String challenge : challenges) {
      CompModule module = CompUtil.parseEverything_fromFile(new A4Reporter(), null, "src/main/resources/challenges/" + challenge);
      List<String> predicates = new java.util.ArrayList<>(module.getAllFunc().makeConstList().stream().map(c -> c.label).toList());
      predicates.remove(predicates.size() - 1);

      arguments.put(challenge, predicates);
    }

    return arguments.entrySet().stream().flatMap(e -> e.getValue().stream().map(p -> Arguments.of(e.getKey().replace(".als", ""), p.replace("this/", ""))));
  }

  @ParameterizedTest
  @MethodSource("getDatasets")
  public void testDBSetup(String challenge, String predicate) {
    Graph g = new Graph(challenge, predicate);
    g.setup();
    g.printStatistics();
  }

}

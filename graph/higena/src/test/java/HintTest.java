import org.higena.ast.TED;
import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.TreeDiff;
import org.higena.graph.Graph;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Record;

import java.util.List;
import java.util.stream.Stream;

public class HintTest {

  private static Stream<Arguments> datasetProvider() {
    return Stream.of(Arguments.of("9jPK8KBWzjFmBx4Hb", "prop1"));
  }

  private static Stream<Arguments> astInputProvider() {
    return Stream.of(Arguments.of("9jPK8KBWzjFmBx4Hb", "prop1"));
  }

  @MethodSource("datasetProvider")
  @ParameterizedTest
  public void printHints(String challenge, String predicate) {

    Graph g = new Graph(challenge, predicate);
    List<Record> exprs = g.runQuery("""
            MATCH (i:Incorrect)
            RETURN i.expr AS expr
            """);

    for (Record rec : exprs) {
      String expr = rec.get("expr").asString();

      System.out.println("\nIncorrect expr: " + expr);
      String hint = g.getTEDHint(expr).toHintMsg();
      System.out.println("HINT:\n" + hint);

    }
  }

  @ValueSource(strings = {"{type='Update', node=&, value=-}"})
  @ParameterizedTest
  public void parseAction(String actionStr) {
    System.out.println(EditAction.fromString(actionStr));

  }

  @MethodSource("astInputProvider")
  @ParameterizedTest
  public void printAllHints(String ast1, String ast2) {
    TED ted = new TED();
    TreeDiff diff = ted.computeTreeDiff(ast1, ast2);

    for (EditAction action : diff.getActions()) {
      System.out.println("Action: " + action);
      System.out.println("Hint: " + EditAction.fromString(action.toString()));
    }
  }
}

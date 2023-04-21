import org.higena.ast.TED;
import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.TreeDiff;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

public class ActionsTest {

  @ValueSource(strings = {"{type='Delete', node='}"})
  @ParameterizedTest
  public void parseAction(String actionStr) {
    System.out.println(EditAction.fromString(actionStr));

  }

  @MethodSource("astInputsProvider")
  @ParameterizedTest
  public void printTreeDiffs(String ast1, String ast2) {
    TED ted = new TED();
    TreeDiff diff = ted.computeTreeDiff(ast1, ast2);

    for (EditAction action : diff.getActions()) {
      System.out.println("Action: " + action);
      System.out.println("Hint: " + EditAction.fromString(action.toString()));
    }
  }

  private static Stream<Arguments> astInputsProvider() {
    return Stream.of(
            Arguments.of("{AND{no{Protected}}{no{Trash}}}", "{AND{no{File" +
                    "}}{no{Trash}}}"));
  }
}

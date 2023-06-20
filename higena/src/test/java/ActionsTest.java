import org.higena.ast.TED;
import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.TreeDiff;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

public class ActionsTest {

  /**
   * Provides arguments for the printTreeDiffs() method. Each argument is a
   * pair of ASTs to compare.
   *
   * @return Arguments for the printTreeDiffs() method.
   */
  private static Stream<Arguments> astInputsProvider() {
    return Stream.of(
            // TreeInsert
            Arguments.of("", "{AND{no{Protected}}}"),
            // Move
            Arguments.of("{<{Protected}{no}}", "{<{no{Protected}}}"),
            // Update
            Arguments.of("{AND{no{Trash}}{no{Protected}}}", "{AND{no{File" +
                    "}}{no{Trash}}}"),
            // TreeDelete
            Arguments.of("{AND{no{Protected}}}", "{AND}"),

            // Bad action
            Arguments.of("{all{one of{var0}{sig/Node}}{!={" +
                    ".{var0/Node}{field/adj{set of{sig/Node}}}}{var0/Node}}}"
                    , "{all{one of{var0}{sig/Node}}{!in{var0/Node}{" +
                            ".{var0/Node}{field/adj{set of{sig/Node}}}}}}"));
  }

  /**
   * Test parsing of EditAction from string representation of action. This
   * method is used to test the fromString() static method of EditAction.
   *
   * @param actionStr String representation of action.
   */
  @ValueSource(strings = {
          "{type='Delete', node='}",
          "{type='TreeDelete', tree='{AND{no{Protected}}{no{Trash}}}'",
          "{type='TreeInsert', tree='{AND{no{Protected}}{no{Trash}}}', " +
                  "parent='root', position=0}",

  })
  @ParameterizedTest
  public void parseAction(String actionStr) {
    System.out.println(EditAction.fromString(actionStr));
  }

  /**
   * Test printing of EditAction to string representation of action. This
   * method is used to test the toString() method of EditAction and the
   * computeTreeDiff() method of TED.
   *
   * @param ast1 First AST to compare.
   * @param ast2 Second AST to compare.
   */
  @MethodSource("astInputsProvider")
  @ParameterizedTest
  public void printTreeDiffs(String ast1, String ast2) {
    TED ted = new TED();
    TreeDiff diff = ted.computeTreeDiff(ast1, ast2);

    for (EditAction action : diff.getActions()) {
      System.out.println("Action: " + action);
    }
  }
}

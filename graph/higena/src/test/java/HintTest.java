import org.higena.ast.TED;
import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.TreeDiff;
import org.higena.graph.Graph;
import org.junit.jupiter.api.Test;

public class HintTest {

  @Test
  public void printAllHints() {

    Graph g = new Graph("9jPK8KBWzjFmBx4Hb", "prop1");
    g.printAllHints();

  }

  @Test
  public void parseAction() {
    String actionStr = "{type='Update', node=&, value=-}";
    System.out.println(EditAction.fromString(actionStr));

  }
}

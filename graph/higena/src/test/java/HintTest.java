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

  @Test
  public void printHints() {
    String tree1 = "{AND{before{no{this/Trash}}}{no{this/Protected}}}",
            tree2 = "{before{no{+{this/Protected}{this/Trash}}}}";
    TED ted = new TED();
    TreeDiff diff = ted.computeTreeDiff(tree1, tree2);
    System.out.println(EditAction.fromString(diff.getActions().get(4).toString()));
    System.out.println(diff.getActions().get(4));
  }
}

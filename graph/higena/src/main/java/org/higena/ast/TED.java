package org.higena.ast;

import at.unisalzburg.dbresearch.apted.costmodel.StringUnitCostModel;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.TreeDiff;
import java.util.List;

/**
 * This class contains auxiliary methods for computing the TED between two trees
 * and the edit actions that transform one tree into the other.
 */
public class TED {
  private final BracketStringInputParser parser;
  private final APTED<StringUnitCostModel, StringNodeData> apted;

  public TED() {
    parser = new BracketStringInputParser();
    apted = new APTED<>(new StringUnitCostModel());
  }

  public TreeDiff computeTreeDiff(String tree1, String tree2) {
    Node<StringNodeData> t1 = parse(tree1), t2 = parse(tree2);
    // Compute TED (must run before computing edits)
    TreeDiff td = new TreeDiff(computeEditDistance(t1, t2));
    // Get edit actions
    MappingStore ms = new AptedMatcher(this).match(t1, t2);
    // Calculate edit actions using Chawathe's algorithm
    EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(ms);
    // Convert edit script actions to EditAction objects
    for (Action action: editScript) {
      td.addAction(new EditAction(action));
    }
    return td;
  }

  public int computeEditDistance(Node<StringNodeData> t1, Node<StringNodeData> t2) {
    return (int) apted.computeEditDistance(t1, t2);
  }

  public List<int[]> computeEdits() {
    return apted.computeEditMapping();
  }

  private Node<StringNodeData> parse(String tree) {
    return parser.fromString(tree);
  }

}

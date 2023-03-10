package org.example.ast;

import at.unisalzburg.dbresearch.apted.costmodel.StringUnitCostModel;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;
import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.InsertDeleteChawatheScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.MappingStore;
import org.example.ast.actions.EditAction;

import java.util.ArrayList;
import java.util.List;

public class TED {
  private final BracketStringInputParser parser;
  private final APTED<StringUnitCostModel, StringNodeData> apted;
  private Node<StringNodeData> t1, t2;

  public TED() {
    parser = new BracketStringInputParser();
    apted = new APTED<>(new StringUnitCostModel());
  }

  public int computeEditDistance(String ast1, String ast2) {
    t1 = parser.fromString(ast1);
    t2 = parser.fromString(ast2);
    return (int) apted.computeEditDistance(t1, t2);
  }

  public List<int[]> computeEdits() {
    return apted.computeEditMapping();
  }

  public List<EditAction> getEdits(String tree1, String tree2) {
    // Get mappings between two trees
    MappingStore ms = new AptedMatcher(this).match(tree1, tree2);
    // Calculate edit actions using Chawathe's algorithm
    EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(ms);
    // Convert edit script actions to EditAction objects
    List<EditAction> edits = new ArrayList<>();

    for (Action action: editScript) {
      edits.add(new EditAction(action));
    }

    return edits;
  }

  public Node<StringNodeData> getTree1() {
    return t1;
  }

  public Node<StringNodeData> getTree2() {
    return t2;
  }
}

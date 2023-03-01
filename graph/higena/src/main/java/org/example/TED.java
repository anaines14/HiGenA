package org.example;

import at.unisalzburg.dbresearch.apted.costmodel.StringUnitCostModel;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;

import java.util.List;

public class TED {
  private final BracketStringInputParser parser;
  private final APTED<StringUnitCostModel, StringNodeData> apted;
  private final StringUnitCostModel costModel;
  private Node<StringNodeData> t1,t2;

  public TED() {
    parser = new BracketStringInputParser();
    costModel = new StringUnitCostModel();
    apted = new APTED<>(costModel);
  }

  public float computeEditDistance(String ast1, String ast2) {
    t1 = parser.fromString(ast1);
    t2 = parser.fromString(ast2);
    return apted.computeEditDistance(t1, t2);
  }

  public List<int[]> computeEdits () {
    return apted.computeEditMapping();
  }

  public static void getEdits(String tree1, String tree2) {
    TED ted = new TED();
    AptedMatcher matcher = new AptedMatcher();
    MappingStore ms = matcher.match(ted, tree1, tree2);

    EditScript actions = new SimplifiedChawatheScriptGenerator().computeActions(ms);
    System.out.println(actions.size() + " actions");
    System.out.println("TED: " + ted.computeEditDistance(tree1, tree2));

    Action a = actions.get(0);
    System.out.println(a.getName());
  }

  public Node<StringNodeData> getTree1() {
    return t1;
  }

  public Node<StringNodeData> getTree2() {
    return t2;
  }
}

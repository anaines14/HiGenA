package org.example;

import at.unisalzburg.dbresearch.apted.costmodel.StringUnitCostModel;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.NodeIndexer;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;

import java.util.List;
import java.util.Map;

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

  public void computeEdits () {
    List<int[]> mapping = apted.computeEditMapping();
    Map<Integer, String> tt1 = new TraversedAST(t1).getIndexNodes();
    Map<Integer, String> tt2 = new TraversedAST(t2).getIndexNodes();

    StringBuilder mappingString = new StringBuilder();
    for (int[] indexes : mapping) {
      if (indexes[0] != 0) {
        mappingString.append(tt1.get(indexes[0])).append(" -- ");
      } else {
        mappingString.append("");
      }

        if (indexes[1] != 0) {
            mappingString.append(tt2.get(indexes[1])).append("\n");
        } else {
            mappingString.append("\n");
        }
    }
    System.out.println(mappingString);
  }

  public static void getEdits(String ast1, String ast2) {
    TED ted = new TED();
    float edit = ted.computeEditDistance(ast1, ast2);

    System.out.println("AST1: " + ast1);
    System.out.println("AST2: " + ast2);
    System.out.println("TED: " + edit);

    ted.computeEdits();
  }
}

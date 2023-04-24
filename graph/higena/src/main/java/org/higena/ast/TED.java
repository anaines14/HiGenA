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
import org.higena.ast.matchers.AptedMatcher;
import org.higena.ast.matchers.GumTreeMatcher;
import org.higena.ast.matchers.TreeMatcher;

import java.util.List;

/**
 * This class contains auxiliary methods for computing the TED between two trees
 * and the edit actions that transform one tree into the other.
 */
public class TED {
  private final BracketStringInputParser parser = new BracketStringInputParser();
  private final APTED<StringUnitCostModel, StringNodeData> apted = new APTED<>(new StringUnitCostModel());
  private final TreeMatcher aptedMatcher = new AptedMatcher(this);
  private final TreeMatcher gumTreeMatcher = new GumTreeMatcher();

  public TED() {
  }

  public TreeDiff computeAptedTreeDiff(String tree1, String tree2) {
    return computeTreeDiff(tree1, tree2, aptedMatcher);
  }

  public TreeDiff computeGumTreeTreeDiff(String tree1, String tree2) {
    return computeTreeDiff(tree1, tree2, gumTreeMatcher);
  }

  public TreeDiff computeTreeDiff(String tree1, String tree2, TreeMatcher matcher) {
    Node<StringNodeData> t1 = parse("{root" + tree1 + "}"), t2 = parse("{root" + tree2 + "}");
    // Compute TED (must run before computing edits)
    TreeDiff td = new TreeDiff(computeEditDistance(t1, t2));
    // Get edit actions
    MappingStore ms = matcher.match(t1, t2);
    // Calculate edit actions using Chawathe's algorithm
    EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(ms);
    // Convert edit script actions to EditAction objects
    for (Action action : editScript) {
      td.addAction(new EditAction(action));
    }
    return td;
  }

  public int computeEditDistance(String t1, String t2) {
    // TODO: Change. This is a workaround for a bug where one string comes with quotes and the other doesn't
    t1 = t1.replace("\"", "");
    t2 = t2.replace("\"", "");

    t1 = "{root" + t1 + "}";
    t2 = "{root" + t2 + "}";
    return computeEditDistance(parse(t1), parse(t2));
  }

  /**
   * Computes the tree edit distance between two trees.
   *
   * @param t1 The first tree
   * @param t2 The second tree
   * @return The tree edit distance between the two trees.
   */
  public int computeEditDistance(Node<StringNodeData> t1, Node<StringNodeData> t2) {
    return (int) apted.computeEditDistance(t1, t2);
  }

  /**
   * Computes the edit mapping between two trees. Must be called after computeEditDistance.
   *
   * @return A list of pairs of node ids. Each pair represents a node in the first tree and a node in the second tree.
   */
  public List<int[]> computeEdits() {
    return apted.computeEditMapping();
  }

  /**
   * Parses a tree from a string.
   *
   * @param tree The string representation of the tree. The format must be
   *             {root{child1}{child2}}
   * @return The parsed tree instance.
   */
  private Node<StringNodeData> parse(String tree) {
    return parser.fromString(tree);
  }

}

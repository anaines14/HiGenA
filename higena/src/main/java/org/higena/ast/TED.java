package org.higena.ast;

import at.unisalzburg.dbresearch.apted.costmodel.StringUnitCostModel;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import org.higena.ast.actions.EditAction;
import org.higena.ast.actions.TreeDiff;

import java.util.List;

/**
 * This class contains methods for computing the differences between two
 * ASTs. This includes computing the Tree Edit Distance (TED) between two
 * trees and computing the edit actions that transform one tree into another.
 * <p>
 * The TED is computed using the APTED library.
 */
public class TED {
  private static final BracketStringInputParser parser =
          new BracketStringInputParser(); // Parser
  private static final APTED<StringUnitCostModel, StringNodeData> apted =
          new APTED<>(new StringUnitCostModel()); // APTED instance

  private static final EditScriptGenerator generator =
          new SimplifiedChawatheScriptGenerator(); // Edit script generator
  public static boolean USE_APTED = true; // True to use APTED; else use GumTree

  public TED() {
  }

  /**
   * Computes the TED between two trees and the edit actions that transform
   * one tree into another.
   *
   * @param tree1 First tree
   * @param tree2 Second tree
   * @return TreeDiff object containing the TED and edit actions
   */
  public TreeDiff computeTreeDiff(String tree1, String tree2) {
    Node<StringNodeData> t1 = parse("{root" + tree1 + "}"), t2 = parse("{root" + tree2 + "}");
    AlloyAST ast1 = new AlloyAST(t1), ast2 = new AlloyAST(t2);

    // Order children of commutative operations to optimize matching
    ast1.prepareForMatching(ast2);

    // Compute TED (must run before computing mappings)
    TreeDiff td = new TreeDiff(computeEditDistance(t1, t2));

    // Get mapping between the two trees
    MappingStore ms;
    if (USE_APTED) { // Use APTED
      ms = aptedMatch(ast1, ast2);
    } else { // Use GumTree
      ms = gumTreeMatch(ast1, ast2);
    }

    // Calculate edit actions using Chawathe's algorithm
    EditScript editScript = generator.computeActions(ms);

    // Convert edit script actions to EditAction objects
    for (Action action : editScript) {
      td.addAction(new EditAction(action));
    }
    return td;
  }

  /**
   * Computes the mapping between two trees using GumTree.
   * @param t1 The first tree
   * @param t2 The second tree
   * @return The mapping between the two trees.
   */
  private MappingStore gumTreeMatch(AlloyAST t1, AlloyAST t2) {
    Matcher defaultMatcher = Matchers.getInstance().getMatcher();
    return defaultMatcher.match(t1, t2);
  }

  /**
   * Computes the mapping between two trees using APTED.
   * @param t1 The first tree
   * @param t2 The second tree
   * @return The mapping between the two trees.
   */
  private MappingStore aptedMatch(AlloyAST t1, AlloyAST t2) {
    MappingStore ms = new MappingStore(t1, t2);

    List<int[]> arrayMappings = this.computeEdits();
    List<Tree> srcs = TreeUtils.postOrder(t1);
    List<Tree> dsts = TreeUtils.postOrder(t2);

    for (int[] m : arrayMappings) {
      if (m[0] != 0 && m[1] != 0) {
        Tree srcg = srcs.get(m[0] - 1);
        Tree dstg = dsts.get(m[1] - 1);

        if (ms.isMappingAllowed(srcg, dstg))
          ms.addMapping(srcg, dstg);
      }
    }

    return ms;
  }

  /**
   * Computes the edit TED between two trees.
   * @param t1 First tree
   * @param t2 Second tree
   * @return TED between the two trees
   */
  public static int computeEditDistance(String t1, String t2) {
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
  public static int computeEditDistance(Node<StringNodeData> t1,
                                        Node<StringNodeData> t2) {
    return (int) apted.computeEditDistance(t1, t2);
  }

  /**
   * Computes the edit mapping between two trees. Must be called after computeEditDistance.
   * @return A list of pairs of node ids. Each pair represents a node in the first tree and a node in the second tree.
   */
  public List<int[]> computeEdits() {
    return apted.computeEditMapping();
  }

  /**
   * Parses a tree from a string.
   * @param tree The string representation of the tree. The format must be
   *             {root{child1}{child2}}
   * @return The parsed tree instance.
   */
  private static Node<StringNodeData> parse(String tree) {
    return parser.fromString(tree);
  }
}

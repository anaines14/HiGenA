package org.higena.parser;

import com.github.gumtreediff.tree.Tree;
import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.*;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import org.higena.ast.AlloyAST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Parses an Alloy expression into an AST.
 */
public class A4FParser {
  public static boolean ANONYMIZE = true;
  public static boolean SORT_COMMUTATIVE = true;

  public static HashMap<String, String> variables = new HashMap<>();

  public static Tree parse(String exprStr, CompModule module) {
    variables.clear();
    Expr expr = CompUtil.parseOneExpression_fromString(module, exprStr);
    Tree tree = parse(expr);
    if (SORT_COMMUTATIVE && tree != null) {
      return Canonicalizer.canonicalize(tree);
    }
    return tree;
  }

  public static Tree parse(String expression, String fullCode) {
    variables.clear();
    // Parse the full module
    CompModule module = CompUtil.parseEverything_fromString(new A4Reporter(),
            fullCode);
    return parse(expression, module);
  }

  private static AlloyAST parse(Expr expr) {

    // Parse the expression based on its type
    switch (expr.getClass().getSimpleName()) {
      case "ExprConstant":
        return parse((ExprConstant) expr);
      case "ExprUnary":
        return parse((ExprUnary) expr);
      case "ExprQt":
        return parse((ExprQt) expr);
      case "ExprVar":
        return parse((ExprVar) expr);
      case "ExprBinary":
        return parse((ExprBinary) expr);
      case "PrimSig":
        return parse((Sig.PrimSig) expr);
      case "SubsetSig":
        return parse((Sig.SubsetSig) expr);
      case "ExprList":
        return parse((ExprList) expr);
      case "ExprITE":
        return parse((ExprITE) expr);
      case "ExprCall":
        return parse((ExprCall) expr);
      case "Field":
        return parse((Sig.Field) expr);
      case "ExprLet":
        return parse((ExprLet) expr);
    }

    return null;
  }

  private static AlloyAST parse(ExprLet expr) {
    String name = "let";
    List<Tree> children = new ArrayList<>();

    children.add(parse(expr.var));
    children.add(parse(expr.sub));

    return new AlloyAST(name, children);
  }

  private static AlloyAST parse(Sig.Field expr) {
    String name = "field/" + expr.label;
    List<Tree> children = new ArrayList<>();

    children.add(parse(expr.decl().expr));

    return new AlloyAST(name, children);
  }

  private static AlloyAST parse(ExprCall expr) {
    String name = "field";
    List<Tree> children = new ArrayList<>();

    // Parse the function
    children.add(parse(expr.fun.getBody()));
    // Parse the arguments
    expr.args.forEach(child -> children.add(parse(child)));

    return new AlloyAST(name, children);
  }

  private static AlloyAST parse(ExprITE expr) {
    String name = "ite";
    List<Tree> children = new ArrayList<>();

    children.add(parse(expr.right));
    children.add(parse(expr.left));

    return new AlloyAST(name, children);
  }

  private static AlloyAST parse(ExprList expr) {
    String name = expr.op.toString();
    List<Tree> children = new ArrayList<>();

    expr.args.forEach(child -> children.add(parse(child)));

    return new AlloyAST(name, children);
  }

  private static AlloyAST parse(Sig.SubsetSig expr) {
    return new AlloyAST(expr.toString().replace("this/", "sig/"));
  }

  private static AlloyAST parse(Sig.PrimSig expr) {
    return new AlloyAST(expr.toString().replace("this/", "sig/"));
  }

  private static AlloyAST parse(ExprBinary expr) {
    String name = expr.op.toString();

    // Parse left and right children
    List<Tree> children = new ArrayList<>();
    children.add(parse(expr.left));
    children.add(parse(expr.right));

    return new AlloyAST(name, children);
  }

  private static AlloyAST parse(ExprVar expr) {
    // Sig : a -> Sig
    String type = expr.explain().split(":")[0].trim();
    // name = var/Sig
    String name;
    if (ANONYMIZE)
      name = variables.get(expr.label) + '/' + type;
    else
      name = expr.label + '/' + type;

    return new AlloyAST(name);
  }

  private static AlloyAST parse(ExprConstant expr) {
    return new AlloyAST(expr.toString());
  }

  private static AlloyAST parse(ExprUnary expr) {
    return parse(expr, null);
  }

  private static AlloyAST parse(ExprUnary expr, String var) {
    List<Tree> children = new ArrayList<>();

    if (expr.op == ExprUnary.Op.NOOP) {
      // Skip to children
      AlloyAST child = parse(expr.sub);
      return new AlloyAST(child.getLabel(), child.getChildren());
    }

    // Parse the children
    String name = expr.op.toString();
    if (var != null) {
      var = var.replace("this/", "");
      if (ANONYMIZE) {
        addVariable(var);
        children.add(new AlloyAST(variables.get(var)));
      } else {
        children.add(new AlloyAST(var));
      }
    }
    children.add(parse(expr.sub));
    return new AlloyAST(name, children);
  }


  private static AlloyAST parse(ExprQt expr) {
    return parse(expr, 0, 0);
  }

  private static AlloyAST parse(ExprQt expr, int declIndex, int varIndex) {
    String name = expr.op.toString();
    List<Tree> children = new ArrayList<>();

    // parse declarations
    if (!expr.decls.isEmpty()) {
      Decl decl = expr.decls.get(declIndex);

      // Add disjoint node
      if (decl.disjoint != null) {
        children.add(new AlloyAST("disj"));
      }
      children.add(parse((ExprUnary) decl.expr, decl.names.get(varIndex).label));

      if (decl.names.size() > varIndex + 1) { // more variables: Qt x, y: Sig
        children.add(parse(expr, declIndex, varIndex + 1));
      } else if (expr.decls.size() > declIndex + 1) { //more declarations: Qt x: Sig, y: Sig
        children.add(parse(expr, declIndex + 1, 0));
      } else {
        children.add(parse(expr.sub));
      }
      return new AlloyAST(name, children);
    }
    // Parse the body
    children.add(parse(expr.sub));
    return new AlloyAST(name, children);
  }

  private static void addVariable(String var) {
    variables.putIfAbsent(var, "var" + variables.size());
  }
}

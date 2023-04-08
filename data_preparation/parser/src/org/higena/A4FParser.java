package org.higena;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.*;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class A4FParser {
  public static HashMap<String, String> variables = new HashMap<>();

  public static A4FNode parse(String exprStr, CompModule module) {
    variables.clear();
    Expr expr = CompUtil.parseOneExpression_fromString(module, exprStr);
    A4FNode tree = parse(expr);
    return tree == null ? null : Canonicalizer.canonicalize(tree);
  }

  public static A4FNode parse(String expr, String full_code) {
    variables.clear();
    // Parse the full module
    CompModule module = CompUtil.parseEverything_fromString(new A4Reporter(),
            full_code);
    return parse(expr, module);
  }

  private static A4FNode parse(Expr expr) {

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

  private static A4FNode parse(ExprLet expr) {
    String name = "let";
    List<A4FNode> children = new ArrayList<>();

    children.add(parse(expr.var));
    children.add(parse(expr.sub));

    return new A4FNode(name, children);
  }

  private static A4FNode parse(Sig.Field expr) {
    String name = "field/" + expr.label;
    List<A4FNode> children = new ArrayList<>();

    children.add(parse(expr.decl().expr));

    return new A4FNode(name, children);
  }

  private static A4FNode parse(ExprCall expr) {
    String name = "field";
    List<A4FNode> children = new ArrayList<>();

    // Parse the function
    children.add(parse(expr.fun.getBody()));
    // Parse the arguments
    expr.args.forEach(child -> children.add(parse(child)));

    return new A4FNode(name, children);
  }

  private static A4FNode parse(ExprITE expr) {
    String name = "ite";
    List<A4FNode> children = new ArrayList<>();

    children.add(parse(expr.right));
    children.add(parse(expr.left));

    return new A4FNode(name, children);
  }

  private static A4FNode parse(ExprList expr) {
    String name = expr.op.toString();
    List<A4FNode> children = new ArrayList<>();

    expr.args.forEach(child -> children.add(parse(child)));

    return new A4FNode(name, children);
  }

  private static A4FNode parse(Sig.SubsetSig expr) {
    return new A4FNode(expr.toString());
  }

  private static A4FNode parse(Sig.PrimSig expr) {
    return new A4FNode(expr.toString());
  }

  private static A4FNode parse(ExprBinary expr) {
    String name = expr.op.toString();

    // Parse left and right children
    List<A4FNode> children = new ArrayList<>();
    children.add(parse(expr.left));
    children.add(parse(expr.right));

    return new A4FNode(name, children);
  }

  private static A4FNode parse(ExprVar expr) {
    String type = expr.type().toString();
    String name = variables.get(expr.label) + '/' + type.substring(1,
            type.length() - 1);

    return new A4FNode(name);
  }

  private static A4FNode parse(ExprConstant expr) {
    return new A4FNode(expr.toString());
  }

  private static A4FNode parse(ExprUnary expr) {
    return parse(expr, null);
  }

  private static A4FNode parse(ExprUnary expr, String var) {
    List<A4FNode> children = new ArrayList<>();

    if (expr.op == ExprUnary.Op.NOOP) {
      // Skip to children
      A4FNode child = parse(expr.sub);
      return new A4FNode(child.getName(), child.getChildren());
    }

    // Parse the children
    String name = expr.op.toString();
    if (var != null) {
      addVariable(var);
      children.add(new A4FNode(variables.get(var)));
    }
    children.add(parse(expr.sub));
    return new A4FNode(name, children);
  }


  private static A4FNode parse(ExprQt expr) {
    String name = expr.op.toString();
    List<A4FNode> children = new ArrayList<>();

    if (!expr.decls.isEmpty()) {
      Decl decl = expr.decls.get(0);
      if (decl.expr instanceof ExprUnary && !decl.names.isEmpty()) {
        children.add(parse((ExprUnary) decl.expr, decl.names.get(0).label));
      }

      if (decl.names.size() > 1) {
        children.add(parse(expr, 1));
      } else {
        children.add(parse(expr.sub));
      }
      return new A4FNode(name, children);
    }
    // Parse the body
    children.add(parse(expr.sub));
    return new A4FNode(name, children);
  }

  private static A4FNode parse(ExprQt expr, int varIndex) {
    String name = expr.op.toString();
    List<A4FNode> children = new ArrayList<>();

    Decl decl = expr.decls.get(0);
    children.add(parse((ExprUnary) decl.expr, decl.names.get(varIndex).label));

    if (decl.names.size() > varIndex + 1) {
      children.add(parse(expr, varIndex + 1));
    } else {
      children.add(parse(expr.sub));
    }
    return new A4FNode(name, children);
  }

  private static void addVariable(String var) {
    variables.putIfAbsent(var, "var" + variables.size());
  }
}


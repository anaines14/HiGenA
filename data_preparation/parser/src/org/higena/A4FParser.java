package org.higena;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.ast.*;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

import java.util.*;

public class A4FParser {

    public static A4FNode parse(String exprStr, CompModule module) {
        Expr expr = CompUtil.parseOneExpression_fromString(module, exprStr);
        A4FNode tree = parse(expr);
        return tree == null ? null : Canonicalizer.canonicalize(tree);
    }

    public static A4FNode parse(String code, String func) {
        // Parse the full module
        CompModule module = CompUtil.parseEverything_fromString(new A4Reporter(), code);

        // Parse each function
        SafeList<Func> functions = module.getAllFunc();
        String funcName;
        for (Func function : functions) {
            if (!function.label.contains("Default")) { // Skip default function
                funcName = function.label.replace("this/", ""); // remove prefix
                if (funcName.equals(func)) {
                    A4FNode tree = parse(function.getBody());
                    return tree == null ? null : Canonicalizer.canonicalize(tree);
                }
            }
        }
        return null;
    }

    public static A4FNode parse(Expr expr) {

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

    public static A4FNode parse(ExprLet expr) {
        String name = "let";
        List<A4FNode> children = new ArrayList<>();

        children.add(parse(expr.var));
        children.add(parse(expr.sub));

        return new A4FNode(name, children);
    }

    public static A4FNode parse(Sig.Field expr) {
        String name = "field";
        List<A4FNode> children = new ArrayList<>();

        children.add(parse(expr.decl().expr));

        return new A4FNode(name, children);
    }

    public static A4FNode parse(ExprCall expr) {
        String name = "field";
        List<A4FNode> children = new ArrayList<>();

        // Parse the function
        children.add(parse(expr.fun.getBody()));
        // Parse the arguments
        expr.args.forEach(child -> children.add(parse(child)));

        return new A4FNode(name, children);
    }

    public static A4FNode parse(ExprITE expr) {
        String name = "ite";
        List<A4FNode> children = new ArrayList<>();

        children.add(parse(expr.right));
        children.add(parse(expr.left));

        return new A4FNode(name, children);
    }

    public static A4FNode parse(ExprList expr) {
        String name = expr.op.toString();
        List<A4FNode> children = new ArrayList<>();

        expr.args.forEach(child -> children.add(parse(child)));

        return new A4FNode(name, children);
    }

    public static A4FNode parse(Sig.SubsetSig expr) {
        return new A4FNode(expr.toString());
    }

    public static A4FNode parse(Sig.PrimSig expr) {
        return new A4FNode(expr.toString());
    }

    public static A4FNode parse(ExprBinary expr) {
        String name = expr.op.toString();

        // Parse left and right children
        List<A4FNode> children = new ArrayList<>();
        children.add(parse(expr.left));
        children.add(parse(expr.right));

        return new A4FNode(name, children);
    }

    public static A4FNode parse(ExprVar expr) {
        String type = expr.type().toString();
        String name = "var/" + type.substring(1, type.length() - 1);

        return new A4FNode(name);
    }

    public static A4FNode parse(ExprConstant expr) {
        return new A4FNode(expr.toString());
    }

    public static A4FNode parse(ExprUnary expr) {
        List<A4FNode> children = new ArrayList<>();
        String name = expr.op.toString();

        if (expr.op == ExprUnary.Op.NOOP) {
            // Skip to children
            A4FNode child = parse(expr.sub);
            return new A4FNode(child.getName(), child.getChildren());
        }

        // Parse the children
        children.add(parse(expr.sub));
        return new A4FNode(name, children);
    }

    public static A4FNode parse(ExprQt expr) {
        String name = expr.op.toString();
        List<A4FNode> children = new ArrayList<>();

        // Parse the declarations
        for (Decl decl : expr.decls) {
            children.add(parse(decl.expr));
        }

        // Parse the body
        children.add(parse(expr.sub));

        return new A4FNode(name, children);
    }
}


package org.higena;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.ast.*;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

import java.util.*;

public class A4FParser {
    public static TreeMap<Integer, A4FAst> parse(String code) {
        TreeMap<Integer, A4FAst> asts = new TreeMap<>();

        A4Reporter rep = new A4Reporter();

        // Parse the full module
        CompModule module = CompUtil.parseEverything_fromString(rep, code);

        // Parse each function
        SafeList<Func> functions = module.getAllFunc();
        String funcName = "";
        for (Func function : functions) {
            if (!function.label.contains("Default")) { // Skip default function
                funcName = function.label.replace("this/prop", ""); // remove prefix
                asts.put(Integer.parseInt(funcName), parseFunc(function)); // Add to map
            }
        }

        return asts;
    }

    public static A4FAst parseFunc(Func func) {
        String name = func.label;
        Expr body = func.getBody();

        A4FNode root = parse(body);

        return new A4FAst(name, root);
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
            default:
                System.out.println("TODO: " + expr.getClass());
        }

        return null;
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

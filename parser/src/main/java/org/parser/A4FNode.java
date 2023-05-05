package org.parser;

import java.util.ArrayList;
import java.util.List;

public class A4FNode {
    private String name;
    private List<A4FNode> children; // subtrees

    public A4FNode(String name) {
        this.name = name;
        this.children = new ArrayList<>();
    }

    public A4FNode (String name, List<A4FNode> children) {
        this.name = name;
        this.children = children;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder(String.format("{%s", name));

        for (A4FNode child : children) {
            res.append(child.toString());
        }

        return res + "}";

    }

    public String getName() {
        return name;
    }

    public List<A4FNode> getChildren() {
        return children;
    }
}

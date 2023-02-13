package org.higena;

public class A4FAst {
    private String name;
    private A4FNode root;

    public A4FAst(String name, A4FNode root) {
        this.name = name;
        this.root = root;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, root);
    }

    public String getName() {
        return name;
    }

    public A4FNode getRoot() {
        return root;
    }
}

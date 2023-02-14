package org.higena;

import java.util.TreeMap;

public class Main {
    public static void main(String[] args) {
/*
        if (args.length != 1) {
            System.out.println("Usage: java -jar a4f.jar <code>");
            System.exit(1);
        }

*/        // Parse the code
        String code = "sig File {\n" +
        "  \t\n" +
        "\tlink : set File\n" +
        "}\n" +
        "\n" +
        "sig Trash in File {}\n" +
        "\n" +
        "sig Protected in File {}\n" +
        "pred inv1 {\n" +
        "\tall f:File|f not in Trash\n" +
        "}\n" +
        "pred inv2 {\n" +
        "\tall f:File|f in Trash\n" +
        "}\n" +
        "pred inv3 {\n" +
        "\tsome f:File|f in Trash\n" +
        "}\n" +
        "pred inv4 {\n" +
        "\tall f:File|f in Protected implies f not in Trash\n" +
        "}\n" +
        "pred inv5 {\n" +
        "\tall f:File|f not in Protected implies f in Trash\n" +
        "}\n" +
        "pred inv6 {\n" +
        "\tall x,y,z : File | (x->y in link and x->z in link) implies z=y\n" +
        "}\n" +
        "\n" +
        "pred isLink[f:File] {\n" +
        "\tall f:File | isLink[f] implies f not in Trash\n" +
        "}\n" +
        "\n" +
        "pred inv7 {\n" +
        "\tall x,y :File | (x->y in link and y->x in link) implies x not in Trash and y not in Trash\n" +
        "}\n" +
        "pred inv8 {\n" +
        "\n" +
        "}\n" +
        "pred inv9 {\n" +
        "\n" +
        "}\n" +
        "pred inv10 {\n" +
        "\n" +
        "}";
        TreeMap<String, A4FAst> asts = A4FParser.parse(code);
    }
}


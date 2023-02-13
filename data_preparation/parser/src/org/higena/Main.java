package org.higena;

import java.util.TreeMap;

public class Main {
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java -jar a4f.jar <code>");
            System.exit(1);
        }

        // Parse the code
        String code = args[0];
        TreeMap<Integer, A4FAst> asts = A4FParser.parse(code);

        // Print the ASTs
        for (A4FAst ast : asts.values()) {
            System.out.println(ast);
        }
    }
}


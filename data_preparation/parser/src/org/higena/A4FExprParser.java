package org.higena;

/*
 * Extracts the text between curly braces of an Alloy predicate.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Pattern;

import static java.lang.System.exit;

public class A4FExprParser {

    private Scanner scanner;
    private Stack<Character> stack;

    public A4FExprParser(String code) {
        try {
            File file = createTempCodeFile(code);
            scanner = new Scanner(file);
            stack = new Stack<>();
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: File not found.");
            exit(1);
        }
    }

    public String parse(String predicateName) {
        String line = scanner.nextLine();
        String expr = "";
        boolean inExpr = false;
        // Find the predicate
        int exprIndex = findWordIndex(predicateName, line) + predicateName.length();
        if (exprIndex == -1) {
            System.err.println("ERROR: Predicate not found.");
            exit(1);
        }

        // Parse the predicate
        for (int i = exprIndex; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '}') {
                stack.pop();
                if (stack.isEmpty()) {
                    return expr;
                }
            }
            if (inExpr) {
                expr += c;
            }
            if (c == '{') {
                inExpr = true;
                stack.push(c);
            }
        }
        return expr;
    }

    private int findWordIndex(String word, String line) {
        boolean start = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == word.charAt(0)) {
                start = true; // Possible start of wor
                for (int j = 1; j < word.length(); j++) {
                    if (line.charAt(i + j) != word.charAt(j)) {
                        start = false; // Not the word
                        break;
                    }
                }
                if (start) { // Found the word
                    return i; // Index of the first character of the word
                }
            }
        }
        return -1;
    }

    private File createTempCodeFile(String code) {
        File file = null;
        try {
            file = File.createTempFile("temp", ".txt");
            file.deleteOnExit();
            Files.write(file.toPath(), code.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

}

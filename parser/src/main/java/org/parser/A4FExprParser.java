package org.parser;

/*
 * Extracts the text between curly braces of an Alloy predicate.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
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
        StringBuilder expr = new StringBuilder();
        boolean inExpr = false;
        // Find the predicate
        int exprIndex =
                findWordIndex(predicateName, line) + predicateName.length();

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
                    return expr.toString();
                }
            }
            if (inExpr) {
                expr.append(c);
            }
            if (c == '{') {
                inExpr = true;
                stack.push(c);
            }
        }
        return expr.toString();
    }

    private int findWordIndex(String word, String line) {
        String regex = word + "\\s*\\{";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            return matcher.start();
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

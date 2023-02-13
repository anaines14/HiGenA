package org.higena;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String code = "\nvar sig File {\n\tvar link : lone File\n}\nvar sig Trash in File {}\nvar sig Protected in File {}\npred prop1 {\n\n}\npred prop2 {\n\n}\npred prop3 {\n\talways some File\n}\npred prop4 {\n\t\n  \teventually some Trash\n}\npred prop5 {\n\t\n\t\n\t \n}\npred prop6 {\n\talways all f:File | f in Trash implies after f in Trash\n}\t\npred prop7 {\n\t\n  \teventually some Protected\n}\npred prop8 {\n\t\n}\npred prop9 {\n\talways all p:Protected | p not in Trash\n}\npred prop10 {\n\t\n}\npred prop11 {\n\talways all f:File | f in File-Protected implies after f in Protected\n}\npred prop12 {\n\t\n}\npred prop13 {\n\n}\npred prop14 {\n\n}\npred prop15 {\n\n}\npred prop16 {\n\n}\npred prop17 {\n\n}\npred prop18 {\n\n}\npred prop19 {\n\n}\npred prop20 {\n\n}";

        Map<String, A4FAst> asts = A4FParser.parse(code);
        //System.out.println(asts.get("this/prop4"));
    }
}


package org.higena;

import org.higena.graph.Graph;
import org.higena.graph.Hint;

import java.util.Scanner;

public class Main {
  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    boolean running = true;
    System.out.println("Welcome to Higena!\n");

    while (running) {
      showMenu();
      switch (scanner.nextInt()) {
        case 1 -> {
          //execSetup();
          System.out.println("Setup database");
        }
        case 2 -> {
          //execHint();
          System.out.println("Get hint");
        }
        case 3 -> {
          running = false;
          System.out.println("Bye!");
        }
        default -> {
          System.out.println("Invalid option. Try again.");
        }
      }
    }
  }


  private static void showMenu() {
    System.out.println("Select an option:");
    System.out.println("1. Setup database");
    System.out.println("2. Get hint");
    System.out.println("3. Exit");
  }
}
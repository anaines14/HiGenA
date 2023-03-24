package org.higena;

import org.higena.util.InteractiveMenu;

public class Main {
  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
      return;
    }
    switch (args[0]) {
      case "-i" -> InteractiveMenu.run();
      default -> usage();
    }
  }

  private static void usage() {
    System.out.println("Usage: higena [-i]");
  }
}
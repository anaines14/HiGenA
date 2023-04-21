package org.higena;

import org.higena.util.InteractiveMenu;

public class Main {
  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
      return;
    }
    if ("-i".equals(args[0])) {
      InteractiveMenu.run();
    } else {
      usage();
    }
  }

  private static void usage() {
    System.out.println("Usage: higena [-i]");
  }
}
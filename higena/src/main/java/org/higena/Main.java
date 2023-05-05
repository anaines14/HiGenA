package org.higena;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import org.higena.graph.Graph;
import org.higena.graph.hint.Hint;
import org.higena.graph.hint.HintGenType;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;


public class Main {
    private static final String CHALLENGES_DIR = "../data/datasets/challenges/";
  public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in);
    System.out.println("Welcome to Higena!\n");

    // Initialize graph db
    String challenge = selectChallenge(scanner);
    String predicate = selectPredicate(scanner, challenge);
    Graph graph = new Graph(challenge.replace(".als", ""), predicate);

    // Main loop
    mainLoop(scanner, graph);
  }

  private static void mainLoop(Scanner scanner, Graph graph) {
    boolean running = true;
    while (running) {
      showMenu();
      switch (scanner.nextInt()) {
        case 1 -> execSetup(graph);
        case 2 -> execHint(scanner, graph);
        case 3 -> {
          running = false;
          System.out.println("Bye!");
        }
        default -> System.out.println("Invalid option. Try again.");
      }
    }
  }

  // Select

  private static String selectChallenge(Scanner scanner) {
    // List challenges
    System.out.println("Select a challenge:");
    List<String> challenges = listChallenges();
    // Select challenge
    int option = getOption(scanner, challenges.size());
    String challenge = challenges.get(option - 1);
    System.out.println("Challenge " + challenge + " selected.");
    return challenge;
  }

  private static String selectPredicate(Scanner scanner, String challenge) {
    // List predicates
    System.out.println("\nSelect a predicate:");
    List<String> predicates = listPredicates(challenge);
    // Select predicate
    int option = getOption(scanner, predicates.size());
    String predicate = predicates.get(option - 1).replace("this/", "");
    System.out.println("Predicate " + predicate + " selected.");
    return predicate;
  }

  // List

  private static List<String> listPredicates(String challenge) {
    CompModule module = CompUtil.parseEverything_fromFile(new A4Reporter(), null, CHALLENGES_DIR + challenge);
    List<String> predicates = module.getAllFunc().makeConstList().stream().map(c -> c.label).toList();

    for (int i = 0; i < predicates.size() - 1; i++) {
      System.out.println((i + 1) + ". " + predicates.get(i).replace("this/", ""));
    }

    return predicates;
  }

  private static List<String> listChallenges() {
    List<String> challenges =
            Arrays.stream(Objects.requireNonNull(new File(CHALLENGES_DIR).list())).toList();

    for (int i = 0; i < challenges.size(); i++) {
      System.out.println((i + 1) + ". " + challenges.get(i).replace(".als", ""));
    }

    return challenges;
  }

  // Execute

  private static void execHint(Scanner scanner, Graph graph) {
    // Clear scanner
    scanner.nextLine();
    // Get expression
    System.out.println("Enter expression:");
    String expression = scanner.nextLine();
    // Choose method
    System.out.println("Select method for generating hint:");
    System.out.println("1. Dijkstra - TED");
    System.out.println("2. Dijkstra - Node popularity");
    System.out.println("3. Dijkstra - Edge popularity");
    int option = getOption(scanner, 3);

    // Get hint
    HintGenType type = switch (option) {
      case 1 -> HintGenType.TED;
      case 2 -> HintGenType.NODE_POISSON;
      case 3 -> HintGenType.REL_POISSON;
      default -> throw new IllegalStateException("Unexpected value: " + option);
    };
    Hint hint = graph.getHint(expression, type);

    if (hint == null)
      System.out.println("No hint found.\n");
  }

  private static void execSetup(Graph graph) {
    graph.setup();
  }

  private static void showMenu() {
    System.out.println("Select an option:");
    System.out.println("1. Setup database");
    System.out.println("2. Get hint");
    System.out.println("3. Exit");
  }

  private static int getOption(Scanner scanner, int max) {
    int option = -1;
    while (option < 1 || option > max) {
      option = scanner.nextInt();
    }
    return option;
  }
}
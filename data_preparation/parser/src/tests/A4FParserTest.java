import org.higena.A4FParser;

public class A4FParserTest {

  public static void main(String[] args) {
    String code = "sig Node { adj : set Node } pred undirected {all n1, n2:Node | n1 in n2.adj implies n2 not in n1.adj " +
            " } pred oriented { all disj n1,n2: Node | n1 in n2.adj implies n2 not in n1.adj } pred acyclic { } pred complete { } pred noLoops { } pred weaklyConnected { } pred stonglyConnected { } pred transitive { }";

    System.out.println(A4FParser.parse("no Node", code));
  }
}

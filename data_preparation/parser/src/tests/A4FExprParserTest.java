import org.higena.A4FExprParser;

public class A4FExprParserTest {

    public static void main(String[] args) {
        A4FExprParser parser = new A4FExprParser("some prop1 { prop1 = 1 }");
        String expr = parser.parse("prop1");
        System.out.println(expr);
    }

}

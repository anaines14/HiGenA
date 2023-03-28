import org.higena.A4FParser;

public class A4FParserTest {

  public static void main(String[] args) {
    String code = "var sig File { var link : lone File } var sig Trash in" + " File {} var sig Protected in File {} pred prop1 {all x:File" + " | some y:File | x in y.link } pred prop2 { all b:File | " + "some n:File | n in b.link } pred prop3 { some File.link}" + " pred prop4 { all x,y:File | x in y.link } pred prop5 {no Trash or all f : File | f in Trash}";

    System.out.println(A4FParser.parse(code, "prop1"));
    System.out.println(A4FParser.parse(code, "prop2"));
    System.out.println(A4FParser.parse(code, "prop3"));
    System.out.println(A4FParser.parse(code, "prop4"));
    System.out.println(A4FParser.parse(code, "prop5"));
  }
}

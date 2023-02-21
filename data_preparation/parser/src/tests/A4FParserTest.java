import org.higena.A4FParser;

public class A4FParserTest {

    public static void main(String[] args) {
        String code = "var sig File { var link : lone File } var sig Trash in File {} var sig Protected in File {} pred prop1 { no Trash+Protected } pred prop2 { no File after some File } pred prop3 { always some File } pred prop4 { eventually some f:File | f in Trash } pred prop5 { eventually some f:File | f not in File' } pred prop6 { always all f:Trash | always f in Trash } pred prop7 { eventually some f:File | f in Protected } pred isLink[f:File]{ some g:File | g->f in link } pred prop8 { always all f:File | isLink[f] implies eventually f.link in Trash } pred prop9 { } pred prop10 { } pred prop11 { } pred prop12 { } pred prop13 { } pred prop14 { } pred prop15 { } pred prop16 { } pred prop17 { } pred prop18 { } pred prop19 { } pred prop20 { }";
        System.out.println(A4FParser.parse(code, "prop8"));
    }
}

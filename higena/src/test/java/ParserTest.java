import org.higena.parser.A4FParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserTest {

  @Test
  void parseTest() {
    String code = "sig Person { Tutors : set Person, Teaches : set Class } sig Group {} sig Class { Groups : Person -> Group } sig Teacher in Person {} sig Student in Person {} pred inv1 { all p : Person | p in Student } pred inv2 { no Teacher } pred inv3 { all p : Person | p not in Student or p not in Teacher } pred inv4 { all p : Person | p in Student or p in Teacher } pred inv5 { some p : Person, c : Class | p in Teacher and p -> c in Teaches } pred inv6 { } pred inv7 { } pred inv8 { } pred inv9 { } pred inv10 { } pred inv11 { } pred inv12 { } pred inv13 { } pred inv14 { } pred inv15 { }";
    String ast = A4FParser.parse("all disj a,b: Person | a != b ", code).toTreeString();

    assertEquals(ast, "{all{disj}{one of{var0}{Person}}{all{disj}{one " +
            "of{var1}{Person}}{!={var0/Person}{var1/Person}}}}");
  }
}

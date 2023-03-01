import org.higena.A4FExprParser;

public class A4FExprParserTest {

    public static void main(String[] args) {
        String code = "abstract sig Source {} sig User extends Source { profile : set Work, visible : set Work } sig Institution extends Source {} sig Id {} sig Work { ids : some Id, source : one Source } pred Inv1 { } pred Inv1o { all u : User | u.visible in u.profile } check Inv1OK { (Inv2o and Inv3o and Inv4o and (some ShouldBeRejected iff (Inv1 and not Inv1o))) implies (Inv1 iff Inv1o) } pred Inv2 { } pred Inv2o { all u : User | u.profile.source in Institution+u } check Inv2OK { (Inv1o and Inv3o and Inv4o and (some ShouldBeRejected iff (Inv2 and not Inv2o))) implies (Inv2 iff Inv2o) } pred Inv3 { } pred Inv3o { all u : User, disj x,y : u.profile | x.source = y.source implies no (x.ids & y.ids) } check Inv3OK { (Inv1o and Inv2o and Inv4o and (some ShouldBeRejected iff (Inv3 and not Inv3o))) implies (Inv3 iff Inv3o) } pred Inv4 { } pred Inv4o { all u : User, disj x,y : u.visible | x not in y.^(ids.~ids) } check Inv4OK { (Inv1o and Inv2o and Inv3o and (some ShouldBeRejected iff (Inv4 and not Inv4o))) implies (Inv4 iff Inv4o) } abstract one sig RejectedBy {} sig ShouldBeRejected extends RejectedBy {} sig ShouldBeAccepted extends RejectedBy {}";
        A4FExprParser parser = new A4FExprParser(code);
        String expr = parser.parse("Inv2");
        System.out.println("Expr: " + expr);
    }

}

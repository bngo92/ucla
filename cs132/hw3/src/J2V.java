import syntaxtree.Node;
import visitor.GJNoArguDepthFirst;

public class J2V {
    public static void main(String[] args) {
        try {
            Node root = new MiniJavaParser(System.in).Goal();
            root.accept(new GJNoArguDepthFirst<Object>());
        } catch (ParseException e) {
            System.out.println(e.toString());
        }
    }
}

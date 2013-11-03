import myjava.MySymbolTable;
import myjava.MyType;
import myjava.MyTypeCheck;
import syntaxtree.Node;

public class Typecheck {
    public static void main(String[] args) {
        Node root = null;
        try {
            root = new MiniJavaParser(System.in).Goal();
        } catch (ParseException e) {
            System.out.println(e.toString());
            System.exit(1);
        }
        // Build the symbol table.
        MySymbolTable pv = new MySymbolTable();
        if (root.accept(pv) == null) {
            System.out.println("Type error");
            System.exit(1);
        } else {
            // Do type checking.
            MyType res = root.accept(new MyTypeCheck(pv));
            if (res != null) {
                System.out.println("Program type checked successfully");
            } else {
                System.out.println("Type error");
                System.exit(1);
            }
        }
    }
}

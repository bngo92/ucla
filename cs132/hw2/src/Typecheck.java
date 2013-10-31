import java.io.*;
import visitor.*;
import syntaxtree.*;
import java.util.*;
import picojava.*;

public class Typecheck {
    public static void main(String[] args) {
        Node root = null;
        try {
            root = new MiniJavaParser(System.in).Goal();
            // Build the symbol table.
            SymTableVis<Void, Integer> pv =
                    new SymTableVis<Void,Integer>();
            try {
                root.accept(pv, 0);
            } catch (Exception e) {
                System.out.println("Type error");
                System.exit(0);
            }
            HashMap<String, String> symt = pv.symt;
            // Do type checking.
            TypeCheckSimp ts = new TypeCheckSimp();
            MyType res = root.accept(ts, symt);
            if (res != null && res.type_array.size() > 0)
                System.out.println("Code typechecks");
            else
                System.out.println("Type error");
        }
        catch (ParseException e) {
            System.out.println(e.toString());
            System.exit(1);
        }
    }
}

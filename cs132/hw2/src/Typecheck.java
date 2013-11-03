import java.io.*;

import syntaxtree.*;
import myjava.*;

public class Typecheck {
    public static void main(String[] args) {
        Node root = null;
        try {
            //root = new MiniJavaParser(System.in).Goal();
            root = new MiniJavaParser(new FileInputStream("C:/Users/Bryan/IdeaProjects/ucla/cs132/hw2/Factorial.java")).Goal();
            // Build the symbol table.
            MySymbolTable pv = new MySymbolTable();
            if (root.accept(pv, null) == null) {
                System.out.println("Type error");
                System.exit(1);
            } else {
                // Do type checking.
                MyType res = root.accept(new MyTypeCheck(), pv);
                if (res != null) {
                    System.out.println("Program type checked successfully");
                } else {
                    System.out.println("Type error");
                    System.exit(1);
                }
            }
        } catch (ParseException e) {
            System.out.println(e.toString());
            System.exit(1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}

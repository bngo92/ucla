import java.io.*;

import syntaxtree.*;
import java.util.*;
import myjava.*;

public class Typecheck {
    public static void main(String[] args) {
        Node root = null;
        try {
            //root = new MiniJavaParser(System.in).Goal();
            root = new MiniJavaParser(new FileInputStream("C:/Users/Bryan/IdeaProjects/ucla/cs132/hw2/Miniexp.java")).Goal();
            // Build the symbol table.
            HashMap<String, String> symt = new HashMap<String, String>();
            // Do type checking.
            MyTypeCheck ts = new MyTypeCheck();
            MyType res = root.accept(ts, symt);
            if (res != null && res.type_array.size() > 0)
                System.out.println("Code typechecks");
            else
                System.out.println("Type error");
        } catch (ParseException e) {
            System.out.println(e.toString());
            System.exit(1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}

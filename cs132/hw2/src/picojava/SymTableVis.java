package picojava;

import java.util.*;
import syntaxtree.*;
import visitor.*;

/**
 * Naive symbol table building. Only considers basic vardecl, integer
 * type, and does not perform any error checking.
 *
 */
public class SymTableVis<R,A> extends GJDepthFirst<R,A> {

    public HashMap<String,String> symt = new HashMap<String,String>();


    public R visit(VarDeclaration n, A argu) throws Exception {
        R _ret=null;
        System.out.println("Processing declaration");

        String type = "";
        switch (n.f0.f0.which) {
            case 2:
                type = "Int"; break;
            default:
                System.out.println("Unsupported case");
        }

        String id = n.f1.f0.tokenImage;

        if (symt.get(id) != null)
            throw new Exception();
        System.out.println("Inserting " + id + " -> " + type);
        // Safe?
        symt.put(id, type);

        return _ret;
    }

}

package myjava;

import java.util.*;
import syntaxtree.*;
import visitor.*;

/**
 * This class implements a basic type checker for Int expressions and
 * assignments. Supported expressions can have + and ( ).
 *
 * WARNING: THIS IS A SIMPLISTIC EXAMPLE you must carefully think
 * about the data structures you need to implement hw2, by no mean the
 * ones used in this example should be considered the ones you must
 * use.
 *
 */
public class MyTypeCheck extends GJDepthFirst<MyType,HashMap<String,String>> {

    public MyType visit(Goal n, HashMap<String,String> arg) {
        HashSet<String> classnames = new HashSet<String>();
        classnames.add(n.f0.f1.f0.tokenImage);
        for (Node node : n.f1.nodes) {
            String s = ((ClassDeclaration) ((TypeDeclaration) node).f0.choice).f1.f0.tokenImage;
            if (classnames.contains(s))
                return null;
            classnames.add(s);
        }

        MyType ret = n.f0.accept(this, arg);
        if (ret == null)
            return null;

        for (Node node : n.f1.nodes) {
            ret = node.accept(this, arg);
            if (ret == null)
                return null;
        }
        return ret;
    }

    public MyType visit(MainClass n, HashMap<String,String> arg) {
        HashMap<String, String> ids = new HashMap<String, String>();
        for (Node node : n.f14.nodes) {
            VarDeclaration var = (VarDeclaration) node;
            String id = var.f1.f0.tokenImage;
            if (ids.containsKey(id))
                return null;
            String type = "";
            switch (var.f0.f0.which) {
                case 2:
                    type = "Int"; break;
                default:
                    System.out.println("Unsupported case");
            }
            ids.put(id, type);
        }

        MyType ret = null;
        for (Node node: n.f15.nodes) {
            ret = node.accept(this, ids); // Statement list
            if (ret == null)
                return null;
        }
        return ret;
    }

    public MyType visit(Statement n, HashMap<String,String> arg) {
        return n.f0.accept(this, arg);
    }


    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public MyType visit(AssignmentStatement n, HashMap<String,String> argu) {
        MyType lhs = n.f0.accept(this, argu);
        MyType rhs = n.f2.accept(this, argu);
        if (lhs != null && rhs != null && lhs.checkIdentical(rhs))
            return new MyType("OK");
        return null;
    }


    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | PrimaryExpression()
     */
    public MyType visit(Expression n, HashMap<String,String> argu) {
        return n.f0.accept(this, argu);
    }


    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | NotExpression()
     *       | BracketExpression()
     */
    public MyType visit(PrimaryExpression n, HashMap<String,String> argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public MyType visit(BracketExpression n, HashMap<String,String> argu) {
        return n.f1.accept(this, argu);
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public MyType visit(PlusExpression n, HashMap<String,String> argu) {
        System.out.println("Visiting PlusExpression");
        MyType oper1 = n.f0.accept(this, argu);
        MyType oper2 = n.f2.accept(this, argu);
        System.out.println("Done Visiting PlusExpression");
        if (oper1 == null)
            System.out.println("Warning: oper1 does not typecheck");
        if (oper2 == null)
            System.out.println("Warning: oper2 does not typecheck");

        // Type inference rule: oper1hint /\ oper2:int => oper1+oper2:int
        if (oper1 != null && oper2 != null && oper1.checkIdentical(oper2) && oper1.type_array.size() == 1)
            return oper1;
        return null;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public MyType visit(IntegerLiteral n, HashMap<String,String> argu) {
        return new MyType("Int");
    }


    /**
     * f0 -> <IDENTIFIER>
     */
    public MyType visit(Identifier n, HashMap<String,String> argu) {
        System.out.println("Visiting Identifier");
        // get the id symbol string:
        String sym = n.f0.tokenImage;
        String type = argu.get(sym);
        System.out.println(sym + " -> " + type);
        return new MyType(type);
    }


    //////////////////////////////////////

    public MyType visit(NodeList n, HashMap<String,String> argu) {
        System.out.println("Visiting nodelist");
        MyType _ret = new MyType();
        for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            MyType val = e.nextElement().accept(this,argu);
            if (val != null && _ret != null)
            {
                _ret.type_array.ensureCapacity(val.type_array.size());
                _ret.type_array.addAll(val.type_array);
            }
            else
                _ret = null;
        }
        return _ret;
    }

    public MyType visit(NodeListOptional n, HashMap<String,String> argu) {
        System.out.println("Visiting nodelistopt");
        if ( n.present() ) {
            MyType _ret = new MyType();
            for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                MyType val = e.nextElement().accept(this,argu);
                if (val != null && _ret != null)
                {
                    _ret.type_array.ensureCapacity(val.type_array.size());
                    _ret.type_array.addAll(val.type_array);
                }
                else
                    _ret = null;
            }
            return _ret;
        }
        else
            return null;
    }


}

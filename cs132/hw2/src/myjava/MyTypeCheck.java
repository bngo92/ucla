package myjava;

import java.util.*;
import syntaxtree.*;
import visitor.*;

public class MyTypeCheck extends GJDepthFirst<MyType, MySymbolTable> {

    @Override
    public MyType visit(Goal n, MySymbolTable argu) {
        if (n.f0.accept(this, argu) == null)
            return null;
        return n.f1.accept(this, argu);
    }

    @Override
    public MyType visit(MainClass n, MySymbolTable argu) {
        argu.setType(n.f1.f0.tokenImage);
        argu.method = "main";
        MyType ret = n.f15.accept(this, argu);
        argu.method = null;
        argu.clearType();
        return ret;
    }

    @Override
    public MyType visit(ClassDeclaration n, MySymbolTable argu) {
        argu.setType(n.f1.f0.tokenImage);
        MyType ret = n.f4.accept(this, argu);
        argu.clearType();
        return ret;
    }

    @Override
    public MyType visit(ClassExtendsDeclaration n, MySymbolTable argu) {
        argu.setType(n.f1.f0.tokenImage);
        MyType ret = n.f6.accept(this, argu);
        argu.clearType();
        return ret;
    }

    @Override
    public MyType visit(MethodDeclaration n, MySymbolTable argu) {
        argu.method = n.f2.f0.tokenImage;
        if (n.f8.accept(this, argu) == null)
            return null;
        MyType retType = n.f1.accept(this, argu);
        MyType ret = n.f10.accept(this, argu);
        if (retType != ret)
            return null;
        argu.method = null;
        return MyType.TRUE;
    }

    @Override
    public MyType visit(Type n, MySymbolTable argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public MyType visit(ArrayType n, MySymbolTable argu) {
        return MyType.ARRAY;
    }

    @Override
    public MyType visit(BooleanType n, MySymbolTable argu) {
        return MyType.BOOLEAN;
    }

    @Override
    public MyType visit(IntegerType n, MySymbolTable argu) {
        return MyType.INTEGER;
    }

    @Override
    public MyType visit(Block n, MySymbolTable argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public MyType visit(AssignmentStatement n, MySymbolTable argu) {
        MyType lhs = n.f0.accept(this, argu);
        MyType rhs = n.f2.accept(this, argu);
        if (lhs != null && rhs != null && argu.isSubclass(rhs, lhs))
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(ArrayAssignmentStatement n, MySymbolTable argu) {
        if (n.f1.accept(this, argu) == MyType.INTEGER &&
                n.f2.accept(this, argu) == MyType.INTEGER &&
                n.f5.accept(this, argu) == MyType.INTEGER)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(IfStatement n, MySymbolTable argu) {
        if (n.f2.accept(this, argu) == MyType.BOOLEAN &&
                n.f4.accept(this, argu) != null &&
                n.f6.accept(this, argu) != null)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(WhileStatement n, MySymbolTable argu) {
        if (n.f2.accept(this, argu) == MyType.BOOLEAN &&
                n.f4.accept(this, argu) != null)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(PrintStatement n, MySymbolTable argu) {
        if (n.f2.accept(this, argu) == MyType.INTEGER)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(AndExpression n, MySymbolTable argu) {
        if (n.f0.accept(this, argu) == MyType.BOOLEAN &&
                n.f2.accept(this, argu) == MyType.BOOLEAN)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(CompareExpression n, MySymbolTable argu) {
        if (n.f0.accept(this, argu) == MyType.INTEGER &&
                n.f2.accept(this, argu) == MyType.INTEGER)
            return MyType.BOOLEAN;
        return null;
    }

    @Override
    public MyType visit(PlusExpression n, MySymbolTable argu) {
        if (n.f0.accept(this, argu) == MyType.INTEGER &&
                n.f2.accept(this, argu) == MyType.INTEGER)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(MinusExpression n, MySymbolTable argu) {
        if (n.f0.accept(this, argu) == MyType.INTEGER &&
                n.f2.accept(this, argu) == MyType.INTEGER)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(TimesExpression n, MySymbolTable argu) {
        if (n.f0.accept(this, argu) == MyType.INTEGER &&
                n.f2.accept(this, argu) == MyType.INTEGER)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(ArrayLookup n, MySymbolTable argu) {
        if (n.f0.accept(this, argu) == MyType.ARRAY &&
                n.f2.accept(this, argu) == MyType.INTEGER)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(ArrayLength n, MySymbolTable argu) {
        if (n.f0.accept(this, argu) == MyType.ARRAY)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(MessageSend n, MySymbolTable argu) {
        MyType type = n.f0.accept(this, argu);
        if (type == null)
            return null;

        String method = n.f2.f0.tokenImage;
        LinkedList<MyType> params = new LinkedList<MyType>(argu.getMethodArgs(method));

        if (n.f4.present()) {
            if (!params.isEmpty())
                return null;

            ExpressionList list = (ExpressionList) n.f4.node;
            if (list.f0.accept(this, argu) != params.remove())
                return null;

            if (list.f1.present()) {
                for (Node node : list.f1.nodes) {
                    if (params.isEmpty())
                        return null;
                    if (node.accept(this, argu) != params.remove())
                        return null;
                }
            }
            if (!params.isEmpty())
                return null;
        }
        return argu.getMethodType(method);
    }

    @Override
    public MyType visit(IntegerLiteral n, MySymbolTable argu) {
        return MyType.INTEGER;
    }

    @Override
    public MyType visit(TrueLiteral n, MySymbolTable argu) {
        return MyType.BOOLEAN;
    }

    @Override
    public MyType visit(FalseLiteral n, MySymbolTable argu) {
        return MyType.BOOLEAN;
    }

    @Override
    public MyType visit(Identifier n, MySymbolTable argu) {
        MyType ret = argu.isVar(n.f0.tokenImage);
        if (ret != null)
            return ret;
        return argu.typeTable.get(n.f0.tokenImage);
    }

    @Override
    public MyType visit(ThisExpression n, MySymbolTable argu) {
        return argu.type;
    }

    @Override
    public MyType visit(ArrayAllocationExpression n, MySymbolTable argu) {
        if (n.f3.accept(this, argu) == MyType.INTEGER)
            return MyType.ARRAY;
        return null;
    }

    @Override
    public MyType visit(AllocationExpression n, MySymbolTable argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public MyType visit(NotExpression n, MySymbolTable argu) {
        if (n.f1.accept(this, argu) == MyType.BOOLEAN)
            return MyType.BOOLEAN;
        return null;
    }

    @Override
    public MyType visit(BracketExpression n, MySymbolTable argu) {
        return n.f1.accept(this, argu);
    }

    //////////////////////////////////////

    @Override
    public MyType visit(TypeDeclaration n, MySymbolTable argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public MyType visit(Statement n, MySymbolTable argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public MyType visit(Expression n, MySymbolTable argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public MyType visit(PrimaryExpression n, MySymbolTable argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public MyType visit(NodeList n, MySymbolTable argu) {
        for (Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
            if (e.nextElement().accept(this, argu) == null)
                return null;
        }
        return MyType.TRUE;
    }

    @Override
    public MyType visit(NodeListOptional n, MySymbolTable argu) {
        if (n.present()) {
            for ( Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
                if (e.nextElement().accept(this, argu) == null)
                    return null;
            }
        }
        return MyType.TRUE;
    }
}

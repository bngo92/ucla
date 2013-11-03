package myjava;

import syntaxtree.*;
import visitor.GJNoArguDepthFirst;

import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;

public class MyTypeCheck extends GJNoArguDepthFirst<MyType> {

    private final MySymbolTable symbolTable;
    boolean typeIdentifier = false;

    public MyTypeCheck(MySymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    public MyType visit(Goal n) {
        if (n.f0.accept(this) == null)
            return null;
        return n.f1.accept(this);
    }

    @Override
    public MyType visit(MainClass n) {
        symbolTable.setClassScope(n.f1.f0.tokenImage);
        symbolTable.methodScope = symbolTable.classScope.getMethod("main");
        MyType ret = n.f15.accept(this);
        symbolTable.methodScope = null;
        symbolTable.clearClassScope();
        return ret;
    }

    @Override
    public MyType visit(ClassDeclaration n) {
        symbolTable.setClassScope(n.f1.f0.tokenImage);
        MyType ret = n.f4.accept(this);
        symbolTable.clearClassScope();
        return ret;
    }

    @Override
    public MyType visit(ClassExtendsDeclaration n) {
        symbolTable.setClassScope(n.f1.f0.tokenImage);
        MyType ret = n.f6.accept(this);
        symbolTable.clearClassScope();
        return ret;
    }

    @Override
    public MyType visit(MethodDeclaration n) {
        symbolTable.methodScope = symbolTable.classScope.getMethod(n.f2.f0.tokenImage);
        if (n.f8.accept(this) == null)
            return null;
        MyType retType = n.f1.accept(this);
        MyType ret = n.f10.accept(this);
        if (retType != ret)
            return null;
        symbolTable.methodScope = null;
        return MyType.TRUE;
    }

    @Override
    public MyType visit(Type n) {
        typeIdentifier = true;
        MyType ret = n.f0.accept(this);
        typeIdentifier = false;
        return ret;
    }

    @Override
    public MyType visit(ArrayType n) {
        return MyType.ARRAY;
    }

    @Override
    public MyType visit(BooleanType n) {
        return MyType.BOOLEAN;
    }

    @Override
    public MyType visit(IntegerType n) {
        return MyType.INTEGER;
    }

    @Override
    public MyType visit(Block n) {
        return n.f1.accept(this);
    }

    @Override
    public MyType visit(AssignmentStatement n) {
        MyType lhs = n.f0.accept(this);
        MyType rhs = n.f2.accept(this);
        if (lhs != null && rhs != null && symbolTable.isSubclass(rhs, lhs))
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(ArrayAssignmentStatement n) {
        if (n.f0.accept(this) == MyType.ARRAY &&
                n.f2.accept(this) == MyType.INTEGER &&
                n.f5.accept(this) == MyType.INTEGER)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(IfStatement n) {
        MyType conditional = n.f2.accept(this);
        MyType ifBlock = n.f4.accept(this);
        MyType elseBlock = n.f6.accept(this);
        if (conditional == MyType.BOOLEAN &&
                ifBlock != null &&
                elseBlock != null)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(WhileStatement n) {
        if (n.f2.accept(this) == MyType.BOOLEAN &&
                n.f4.accept(this) != null)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(PrintStatement n) {
        if (n.f2.accept(this) == MyType.INTEGER)
            return MyType.TRUE;
        return null;
    }

    @Override
    public MyType visit(AndExpression n) {
        if (n.f0.accept(this) == MyType.BOOLEAN &&
                n.f2.accept(this) == MyType.BOOLEAN)
            return MyType.BOOLEAN;
        return null;
    }

    @Override
    public MyType visit(CompareExpression n) {
        if (n.f0.accept(this) == MyType.INTEGER &&
                n.f2.accept(this) == MyType.INTEGER)
            return MyType.BOOLEAN;
        return null;
    }

    @Override
    public MyType visit(PlusExpression n) {
        if (n.f0.accept(this) == MyType.INTEGER &&
                n.f2.accept(this) == MyType.INTEGER)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(MinusExpression n) {
        if (n.f0.accept(this) == MyType.INTEGER &&
                n.f2.accept(this) == MyType.INTEGER)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(TimesExpression n) {
        if (n.f0.accept(this) == MyType.INTEGER &&
                n.f2.accept(this) == MyType.INTEGER)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(ArrayLookup n) {
        if (n.f0.accept(this) == MyType.ARRAY &&
                n.f2.accept(this) == MyType.INTEGER)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(ArrayLength n) {
        if (n.f0.accept(this) == MyType.ARRAY)
            return MyType.INTEGER;
        return null;
    }

    @Override
    public MyType visit(MessageSend n) {
        MyType type = n.f0.accept(this);
        if (type == null)
            return null;

        String method = n.f2.f0.tokenImage;
        Collection<MyType> params = type.getMethodArgs(method);

        if (n.f4.present()) {
            if (params == null)
                return null;
            LinkedList<MyType> queue = new LinkedList<MyType>(params);

            ExpressionList list = (ExpressionList) n.f4.node;
            if (!symbolTable.isSubclass(list.f0.accept(this), queue.remove()))
                return null;

            if (list.f1.present()) {
                for (Node node : list.f1.nodes) {
                    if (queue.isEmpty())
                        return null;
                    if (node.accept(this) != queue.remove())
                        return null;
                }
            }
            if (!queue.isEmpty())
                return null;
        } else if (!params.isEmpty())
            return null;
        return type.getMethodType(method);
    }

    @Override
    public MyType visit(IntegerLiteral n) {
        return MyType.INTEGER;
    }

    @Override
    public MyType visit(TrueLiteral n) {
        return MyType.BOOLEAN;
    }

    @Override
    public MyType visit(FalseLiteral n) {
        return MyType.BOOLEAN;
    }

    @Override
    public MyType visit(Identifier n) {
        if (typeIdentifier)
            return symbolTable.classTable.get(n.f0.tokenImage);
        return symbolTable.getVarType(n.f0.tokenImage);
    }

    @Override
    public MyType visit(ThisExpression n) {
        return symbolTable.classScope;
    }

    @Override
    public MyType visit(ArrayAllocationExpression n) {
        if (n.f3.accept(this) == MyType.INTEGER)
            return MyType.ARRAY;
        return null;
    }

    @Override
    public MyType visit(AllocationExpression n) {
        typeIdentifier = true;
        MyType ret = n.f1.accept(this);
        typeIdentifier = false;
        return ret;
    }

    @Override
    public MyType visit(NotExpression n) {
        if (n.f1.accept(this) == MyType.BOOLEAN)
            return MyType.BOOLEAN;
        return null;
    }

    @Override
    public MyType visit(BracketExpression n) {
        return n.f1.accept(this);
    }

    //////////////////////////////////////

    @Override
    public MyType visit(TypeDeclaration n) {
        return n.f0.choice.accept(this);
    }

    @Override
    public MyType visit(Statement n) {
        return n.f0.choice.accept(this);
    }

    @Override
    public MyType visit(Expression n) {
        return n.f0.accept(this);
    }

    @Override
    public MyType visit(ExpressionRest n) {
        return n.f1.accept(this);
    }

    @Override
    public MyType visit(PrimaryExpression n) {
        return n.f0.accept(this);
    }

    @Override
    public MyType visit(NodeList n) {
        for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            if (e.nextElement().accept(this) == null)
                return null;
        }
        return MyType.TRUE;
    }

    @Override
    public MyType visit(NodeListOptional n) {
        if (n.present()) {
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                if (e.nextElement().accept(this) == null)
                    return null;
            }
        }
        return MyType.TRUE;
    }
}

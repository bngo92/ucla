import syntaxtree.*;
import visitor.DepthFirstVisitor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class J2V extends DepthFirstVisitor {
    public static void main(String[] args) {
        try {
            Node root = new MiniJavaParser(new FileInputStream("cs132/hw3/Factorial.java")).Goal();
            root.accept(new J2V());
        } catch (ParseException e) {
            System.out.println(e.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    String classScope;
    String methodScope;
    int varCounter;
    int indent;
    String lastExpression;
    String something;

    public void print(String s, Object... arg) {
        System.out.print(String.format(s, arg));
    }

    public void printIndent(String s, Object... arg) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < indent * 2; i++)
            ret.append(' ');
        print(ret + s, arg);
    }

    @Override
    public void visit(MainClass n) {
        varCounter = 0;

        printIndent("func Main()\n");
        indent++;
        n.f15.accept(this);
        printIndent("ret\n");
        indent--;
        print("\n");
    }

    @Override
    public void visit(ClassDeclaration n) {
        classScope = n.f1.f0.tokenImage;
        n.f4.accept(this);
        classScope = null;
    }

    @Override
    public void visit(ClassExtendsDeclaration n) {
        classScope = n.f1.f0.tokenImage;
        n.f6.accept(this);
        classScope = null;
    }

    @Override
    public void visit(MethodDeclaration n) {
        varCounter = 0;

        n.f4.accept(this);
        methodScope = String.format("%s.%s" ,classScope, n.f2.f0.tokenImage);
        printIndent("func (this%s)\n", lastExpression);
        indent++;
        n.f8.accept(this);
        n.f10.accept(this);
        printIndent("ret %s\n", lastExpression);
        indent--;
    }

    @Override
    public void visit(FormalParameterList n) {
        n.f0.accept(this);
        String expression = " " + lastExpression;
        for (Node node : n.f1.nodes) {
            node.accept(this);
            expression += " " + lastExpression;
        }
        lastExpression = expression;
    }

    @Override
    public void visit(ArrayType n) {
    }

    @Override
    public void visit(BooleanType n) {
    }

    @Override
    public void visit(IntegerType n) {
    }

    @Override
    public void visit(Block n) {
    }

    @Override
    public void visit(AssignmentStatement n) {
        n.f2.accept(this);
        printIndent(String.format("%s = %s\n", n.f0.f0.tokenImage, lastExpression));
    }

    @Override
    public void visit(ArrayAssignmentStatement n) {
    }

    @Override
    public void visit(IfStatement n) {
        n.f2.accept(this);
        printIndent("if0 %s goto :if1_else\n", lastExpression);
        indent++;
        n.f4.accept(this);
        printIndent("goto :if1_end\n");
        indent--;
        printIndent("if1_else:\n");
        indent++;
        n.f6.accept(this);
        indent--;
        printIndent("if1_end\n");
    }

    @Override
    public void visit(WhileStatement n) {
    }

    @Override
    public void visit(PrintStatement n) {
        n.f2.accept(this);
        printIndent("PrintIntS(%s)\n", lastExpression);
    }

    @Override
    public void visit(Expression n) {
        n.f0.accept(this);
        if (lastExpression.contains(" ")) {
            printIndent("t.%d = %s\n", varCounter, lastExpression);
            lastExpression = String.format("t.%d", varCounter);
            ++varCounter;
        }
    }

    @Override
    public void visit(AndExpression n) {
    }

    @Override
    public void visit(CompareExpression n) {
        n.f0.accept(this);
        String lhs = lastExpression;
        n.f2.accept(this);
        String rhs = lastExpression;
        lastExpression = String.format("LtS(%s %s)", lhs, rhs);
    }

    @Override
    public void visit(PlusExpression n) {
    }

    @Override
    public void visit(MinusExpression n) {
        n.f0.accept(this);
        String op1 = lastExpression;
        n.f2.accept(this);
        String op2 = lastExpression;
        lastExpression = String.format("Sub(%s %s)", op1, op2);
    }

    @Override
    public void visit(TimesExpression n) {
        n.f0.accept(this);
        String op1 = lastExpression;
        n.f2.accept(this);
        String op2 = lastExpression;
        lastExpression = String.format("MulS(%s %s)", op1, op2);
    }

    @Override
    public void visit(ArrayLookup n) {
    }

    @Override
    public void visit(ArrayLength n) {
    }

    @Override
    public void visit(MessageSend n) {
        n.f0.accept(this);
        String callInstance = lastExpression;
        n.f4.accept(this);
        lastExpression = String.format("call :%s(%s%s)", something, callInstance, lastExpression);
    }

    @Override
    public void visit(ExpressionList n) {
        n.f0.accept(this);
        String expression = " " + lastExpression;
        for (Node node : n.f1.nodes) {
            node.accept(this);
            expression += " " + lastExpression;
        }
        lastExpression = expression;
    }

    @Override
    public void visit(IntegerLiteral n) {
        lastExpression = n.f0.tokenImage;
    }

    @Override
    public void visit(TrueLiteral n) {
    }

    @Override
    public void visit(FalseLiteral n) {
    }

    @Override
    public void visit(Identifier n) {
        lastExpression = n.f0.tokenImage;
    }

    @Override
    public void visit(ThisExpression n) {
        lastExpression = "this";
        something = methodScope;
    }

    @Override
    public void visit(ArrayAllocationExpression n) {
    }

    @Override
    public void visit(AllocationExpression n) {
        lastExpression = ":empty_fac";
        something = n.f1.f0.tokenImage;
    }

    @Override
    public void visit(NotExpression n) {
    }
}

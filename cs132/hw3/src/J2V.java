import syntaxtree.*;
import visitor.DepthFirstVisitor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.HashSet;

public class J2V extends DepthFirstVisitor {
    public static void main(String[] args) {
        try {
            Node root = new MiniJavaParser(new FileInputStream("cs132/hw3/LinearSearch.java")).Goal();
            root.accept(new J2V());
        } catch (ParseException e) {
            System.out.println(e.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    HashSet<String> classWithVar;
    ArrayDeque<String> strings;
    String classScope;
    String methodScope;
    int varCounter;
    int indent;
    String lastExpression;
    boolean simple;
    String something;

    public void print(String s, Object... arg) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < indent * 2; i++)
            ret.append(' ');
        strings.add(String.format(ret + s, arg));
    }

    @Override
    public void visit(Goal n) {
        classWithVar = new HashSet<String>();
        for (Node node : n.f1.nodes) {
            TypeDeclaration type = (TypeDeclaration) node;
            if (type.f0.which == 0) {
                ClassDeclaration c = (ClassDeclaration) type.f0.choice;
                if (c.f3.present())
                    classWithVar.add(c.f1.f0.tokenImage);
            } else {
                ClassExtendsDeclaration c = (ClassExtendsDeclaration) type.f0.choice;
                if (c.f5.present())
                    classWithVar.add(c.f1.f0.tokenImage);
            }
        }

        strings = new ArrayDeque<String>();
        n.f0.accept(this);
        n.f1.accept(this);
        strings.removeLast();
        for (String s: strings)
            System.out.println(s);
    }

    @Override
    public void visit(MainClass n) {
        varCounter = 0;

        print("func Main()");
        indent++;
        n.f15.accept(this);
        print("ret");
        indent--;
        print("");
    }

    @Override
    public void visit(ClassDeclaration n) {
        classScope = n.f1.f0.tokenImage;
        System.out.println(String.format("const empty_%s", classScope));
        System.out.println("");
        System.out.println("");
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
        methodScope = String.format("%s.%s", classScope, n.f2.f0.tokenImage);
        print("func %s(this%s)", methodScope, lastExpression);
        indent++;
        n.f8.accept(this);
        n.f10.accept(this);
        print("ret %s", lastExpression);
        indent--;
        print("");
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
        simple = false;
        n.f2.accept(this);
        if (simple) {
            String lastString = strings.removeLast().trim();
            print("%s%s", n.f0.f0.tokenImage, lastString.substring(lastString.indexOf(" ")));
            varCounter--;
        } else {
            print("%s = %s", n.f0.f0.tokenImage, lastExpression);
        }
    }

    @Override
    public void visit(ArrayAssignmentStatement n) {
    }

    @Override
    public void visit(IfStatement n) {
        n.f2.accept(this);
        print("if0 %s goto :if1_else", lastExpression);
        indent++;
        n.f4.accept(this);
        print("goto :if1_end");
        indent--;
        print("if1_else:");
        indent++;
        n.f6.accept(this);
        indent--;
        print("if1_end:");
    }

    @Override
    public void visit(WhileStatement n) {
    }

    @Override
    public void visit(PrintStatement n) {
        n.f2.accept(this);
        print("PrintIntS(%s)", lastExpression);
    }

    @Override
    public void visit(Expression n) {
        n.f0.accept(this);
        if (lastExpression.contains(" ")) {
            print("t.%d = %s", varCounter, lastExpression);
            lastExpression = String.format("t.%d", varCounter);
            ++varCounter;
            simple = true;
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
        lastExpression = "";
        n.f4.accept(this);
        lastExpression = String.format("call :%s.%s(%s%s)", something, n.f2.f0.tokenImage, callInstance, lastExpression);
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
        something = classScope;
    }

    @Override
    public void visit(ArrayAllocationExpression n) {
    }

    @Override
    public void visit(AllocationExpression n) {
        something = n.f1.f0.tokenImage;
        if (classWithVar.contains(something)) {
            lastExpression = String.format("t.%d", varCounter);
            varCounter++;
            print("%s = HeapAllocZ(8)", lastExpression);
            print("if %s goto :null1", lastExpression);
            indent++;
            print("Error(\"null pointer\")");
            indent--;
            print("null1:");
        } else {
            lastExpression = String.format(":empty_%s", something);
        }
    }

    @Override
    public void visit(NotExpression n) {
    }
}

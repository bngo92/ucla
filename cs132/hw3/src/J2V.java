import syntaxtree.*;
import visitor.DepthFirstVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class J2V extends DepthFirstVisitor {
    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(new File("cs132/hw3/LinkedList.opt.vapor")));
            Node root = new MiniJavaParser(new FileInputStream("cs132/hw3/LinkedList.java")).Goal();
            root.accept(new J2V());
        } catch (ParseException e) {
            System.out.println(e.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    enum VarType {
        NONE, REFERENCE, ARRAY
    }

    class Var {
        String classname;
        String var;
        VarType type;

        Var(String classname, String var, Type type) {
            this.classname = classname;
            this.var = var;
            switch (type.f0.which) {
                case 3:
                    this.type = VarType.REFERENCE;
                    break;
                case 0:
                    this.type = VarType.ARRAY;
                    break;
                default:
                    this.type = VarType.NONE;
                    break;
            }
        }
    }

    HashMap<String, LinkedHashMap<String, Var>> classVars;
    HashMap<String, Integer> classSize;
    LinkedHashMap<String, Var> methodVars;

    ArrayDeque<String> strings;
    int indent;

    String classScope;
    String methodScope;

    int varCount;
    int boundCount = 1;
    int ifCount = 1;
    int nullCount = 1;
    int whileCount = 1;
    int ssCount = 1;

    String lastExpression;
    boolean access;
    boolean allocArray;
    boolean local;
    String objClass;
    boolean not;

    public void print(String s, Object... arg) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < indent * 2; i++)
            ret.append(' ');
        strings.add(String.format(ret + s, arg));
    }

    private void printArrayAlloc() {
        print("func AllocArray(size)");
        indent++;
        print("bytes = MulS(size 4)");
        print("bytes = Add(bytes 4");
        print("v = HeapAllocZ(bytes)");
        print("[v] = size");
        print("ret v");
        indent--;
        print("");
    }

    @Override
    public void visit(Goal n) {
        classVars = new HashMap<String, LinkedHashMap<String, Var>>();
        classSize = new HashMap<String, Integer>();
        for (Node node : n.f1.nodes) {
            TypeDeclaration type = (TypeDeclaration) node;
            NodeListOptional list;
            String name;
            if (type.f0.which == 0) {
                name = ((ClassDeclaration) type.f0.choice).f1.f0.tokenImage;
                list = ((ClassDeclaration) type.f0.choice).f3;
            } else {
                name = ((ClassExtendsDeclaration) type.f0.choice).f1.f0.tokenImage;
                list = ((ClassExtendsDeclaration) type.f0.choice).f5;
            }

            LinkedHashMap<String, Var> vars = new LinkedHashMap<String, Var>();
            int offset = 0;
            for (Node varNode : list.nodes) {
                VarDeclaration var = (VarDeclaration) varNode;
                vars.put(var.f1.f0.tokenImage, new Var(name, String.format("[this+%d]", offset), var.f0));
                offset += 4;
            }
            classVars.put(name, vars);
            classSize.put(name, offset);
        }

        strings = new ArrayDeque<String>();
        System.out.println("");
        n.f0.accept(this);
        n.f1.accept(this);
        System.out.println("");

        if (allocArray)
            printArrayAlloc();
        strings.removeLast();
        for (String s: strings)
            System.out.println(s);
    }

    @Override
    public void visit(MainClass n) {
        varCount = 0;

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
        n.f4.accept(this);
    }

    @Override
    public void visit(ClassExtendsDeclaration n) {
        classScope = n.f1.f0.tokenImage;
        System.out.println(String.format("const empty_%s", classScope));
        System.out.println("");
        n.f6.accept(this);
    }

    @Override
    public void visit(VarDeclaration n) {
        n.f0.accept(this);
        if (!methodVars.containsKey(n.f1.f0.tokenImage))
            methodVars.put(n.f1.f0.tokenImage, new Var(classScope, n.f1.f0.tokenImage, n.f0));
    }

    @Override
    public void visit(MethodDeclaration n) {
        varCount = 0;

        methodVars = new LinkedHashMap<String, Var>();
        lastExpression = "";
        n.f4.accept(this);
        methodScope = String.format("%s.%s", classScope, n.f2.f0.tokenImage);
        print("func %s(this%s)", methodScope, lastExpression);

        n.f7.accept(this);

        indent++;
        n.f8.accept(this);
        access = false;
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
    public void visit(FormalParameter n) {
        n.f0.accept(this);
        methodVars.put(n.f1.f0.tokenImage, new Var(lastExpression, n.f1.f0.tokenImage, n.f0));
        lastExpression = n.f1.f0.tokenImage;
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
    public void visit(AssignmentStatement n) {
        access = false;
        n.f0.accept(this);
        String lhs = lastExpression;
        local = false;
        n.f2.accept(this);
        local = true;
        print("%s = %s", lhs, lastExpression);
    }

    @Override
    public void visit(ArrayAssignmentStatement n) {
    }

    @Override
    public void visit(IfStatement n) {
        access = true;
        int ifCount = this.ifCount++;
        not = false;
        n.f2.accept(this);
        if (not)
            print("if %s goto :if%d_else", lastExpression, ifCount);
        else
            print("if0 %s goto :if%d_else", lastExpression, ifCount);
        indent++;
        n.f4.accept(this);
        print("goto :if%d_end", ifCount);
        indent--;
        print("if%d_else:", ifCount);
        indent++;
        n.f6.accept(this);
        indent--;
        print("if%d_end:", ifCount);
    }

    @Override
    public void visit(WhileStatement n) {
        int whileCount = this.whileCount++;
        print("while%d_top:", whileCount);
        local = true;
        n.f2.accept(this);
        print("if0 %s goto :while%d_end", lastExpression, whileCount);
        indent++;
        n.f4.accept(this);
        print("goto :while%d_top", whileCount);
        indent--;
        print("while%d_end:", whileCount);
    }

    @Override
    public void visit(PrintStatement n) {
        access = true;
        n.f2.accept(this);
        print("PrintIntS(%s)", lastExpression);
    }

    @Override
    public void visit(Expression n) {
        n.f0.accept(this);
        if (access && lastExpression.contains(" ")) {
            String t = String.format("t.%d", varCount++);
            print("%s = %s", t, lastExpression);
            lastExpression = t;
        }
    }

    @Override
    public void visit(AndExpression n) {
        n.f0.accept(this);
        int ss = ssCount++;
        print("if %s goto :ss%d_else", lastExpression, ss);
        n.f2.accept(this);
        int varCount = this.varCount++;
        indent++;
        print("t.%d = Sub(1 %s)", varCount, lastExpression);
        print("goto :ss%d_end", ss);
        indent--;
        print("ss%d_else:", ss);
        indent++;
        lastExpression = String.format("t.%d", varCount);
        print("%s = 0", lastExpression);
        indent--;
        print("ss%d_end:", ss);
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
        n.f0.accept(this);
        String op1 = lastExpression;
        n.f2.accept(this);
        String op2 = lastExpression;
        lastExpression = String.format("Add(%s %s)", op1, op2);
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
        n.f0.accept(this);
        String t1 = lastExpression;
        int nullCount = this.nullCount++;
        print("if %s goto :null%d", t1, nullCount);
        indent++;
        print("Error(\"null pointer\")");
        indent--;
        print("null%d:", nullCount);

        String t2 = String.format("t.%d", varCount++);
        print("%s = [%s]", t2, t1);
        n.f2.accept(this);
        print("%s = Lt(%s %s)", t2, lastExpression, t2);

        int boundCount = this.boundCount++;
        print("if %s goto :bounds%d", t2, boundCount);
        indent++;
        print("Error(\"array index out of bounds\")");
        indent--;
        print("bounds%d:", boundCount);

        print("%s = MulS(%s 4)", t2, lastExpression);
        print("%s = Add(%s %s)", t2, t2, t1);

        String t3 = String.format("t.%d", varCount++);
        print("%s = [%s+4]", t3, t2);
        lastExpression = t3;
    }

    @Override
    public void visit(ArrayLength n) {
    }

    @Override
    public void visit(MessageSend n) {
        Boolean save = access;
        access = true;
        n.f0.accept(this);

        String callInstance = lastExpression;
        lastExpression = "";
        access = save;
        n.f4.accept(this);
        lastExpression = String.format("call :%s.%s(%s%s)", objClass, n.f2.f0.tokenImage, callInstance, lastExpression);
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
    public void visit(PrimaryExpression n) {
        n.f0.accept(this);
        if (local && lastExpression.contains("+")) {
            print("t.%d = %s", varCount, lastExpression);
            lastExpression = String.format("t.%d", varCount++);
        }
    }

    @Override
    public void visit(IntegerLiteral n) {
        lastExpression = n.f0.tokenImage;
    }

    @Override
    public void visit(TrueLiteral n) {
        lastExpression = "1";
    }

    @Override
    public void visit(FalseLiteral n) {
        lastExpression = "0";
    }

    @Override
    public void visit(Identifier n) {
        lastExpression = n.f0.tokenImage;
        Var var = classVars.get(classScope).get(lastExpression);
        if (var != null) {
            if (access && var.type == VarType.REFERENCE) {
                print("if %s goto :null%d", var.var, nullCount);
                indent++;
                print("Error(\"null pointer\")");
                indent--;
                print("null%d:", nullCount++);
            }
            lastExpression = var.var;
            return;
        }

        var = methodVars.get(lastExpression);
        if (var != null) {
            if (access && var.type == VarType.REFERENCE) {
                print("if %s goto :null%d", var.var, nullCount);
                indent++;
                print("Error(\"null pointer\")");
                indent--;
                print("null%d:", nullCount++);
            }
            objClass = var.classname;
            lastExpression = var.var;
        }
    }

    @Override
    public void visit(ThisExpression n) {
        lastExpression = "this";
        objClass = classScope;
    }

    @Override
    public void visit(ArrayAllocationExpression n) {
        allocArray = true;
        n.f3.accept(this);
        print("t.%d = call :AllocArray(%s)", varCount, lastExpression);
        lastExpression = String.format("t.%d", varCount++);
    }

    @Override
    public void visit(AllocationExpression n) {
        objClass = n.f1.f0.tokenImage;
        if (classVars.get(objClass).size() != 0) {
            lastExpression = String.format("HeapAllocZ(%d)", classSize.get(objClass));
        } else {
            lastExpression = String.format(":empty_%s", objClass);
        }
    }

    @Override
    public void visit(NotExpression n) {
        n.f1.accept(this);
        not = true;
    }
}

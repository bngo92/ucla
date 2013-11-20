import syntaxtree.*;
import visitor.DepthFirstVisitor;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class J2V extends DepthFirstVisitor {
    private boolean eval;

    public static void main(String[] args) {
        try {
            Node root = new MiniJavaParser(System.in).Goal();
            root.accept(new J2V());
        } catch (ParseException e) {
            System.out.println(e.toString());
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
    boolean reference;
    boolean allocArray;
    boolean local;
    String objClass;
    boolean not;
    boolean newAlloc;

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
        print("bytes = Add(bytes 4)");
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
        if (!methodVars.containsKey(n.f1.f0.tokenImage)) {
            if (n.f0.f0.which == 3)
                methodVars.put(n.f1.f0.tokenImage, new Var(((Identifier) n.f0.f0.choice).f0.tokenImage, n.f1.f0.tokenImage, n.f0));
            else
                methodVars.put(n.f1.f0.tokenImage, new Var("", n.f1.f0.tokenImage, n.f0));
        }
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

        reference = false;
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
        reference = false;
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
        int ifCount = this.ifCount++;
        reference = true;
        not = false;
        local = true;

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

        not = true;
        n.f2.accept(this);
        if (not) {
            String var = String.format("t.%d", varCount++);
            print("%s = Sub(1 %s)", var, lastExpression);
            lastExpression = var;
        }
        print("if0 %s goto :while%d_end", lastExpression, whileCount);
        indent++;

        n.f4.accept(this);
        print("goto :while%d_top", whileCount);
        indent--;
        print("while%d_end:", whileCount);
    }

    @Override
    public void visit(PrintStatement n) {
        local = true;
        n.f2.accept(this);
        print("PrintIntS(%s)", lastExpression);
    }

    @Override
    public void visit(Expression n) {
        n.f0.accept(this);
        if ((local || eval) && lastExpression.contains(" ")) {
            String t = String.format("t.%d", varCount++);
            print("%s = %s", t, lastExpression);
            lastExpression = t;
        }
    }

    @Override
    public void visit(AndExpression n) {
        Boolean savedEval = eval;
        eval = true;
        n.f0.accept(this);

        int ss = ssCount++;
        print("if %s goto :ss%d_else", lastExpression, ss);

        eval = true;
        n.f2.accept(this);
        eval = savedEval;

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
        not = false;
    }

    @Override
    public void visit(CompareExpression n) {
        Boolean savedEval = eval;
        eval = true;
        n.f0.accept(this);
        String lhs = lastExpression;

        eval = true;
        n.f2.accept(this);
        eval = savedEval;
        String rhs = lastExpression;
        lastExpression = String.format("LtS(%s %s)", lhs, rhs);
        not = false;
    }

    @Override
    public void visit(PlusExpression n) {
        Boolean savedEval = eval;
        eval = true;
        n.f0.accept(this);
        String op1 = lastExpression;

        eval = true;
        n.f2.accept(this);
        eval = savedEval;
        String op2 = lastExpression;
        lastExpression = String.format("Add(%s %s)", op1, op2);
    }

    @Override
    public void visit(MinusExpression n) {
        Boolean savedEval = eval;
        eval = true;
        n.f0.accept(this);
        String op1 = lastExpression;

        eval = true;
        n.f2.accept(this);
        eval = savedEval;
        String op2 = lastExpression;
        lastExpression = String.format("Sub(%s %s)", op1, op2);
    }

    @Override
    public void visit(TimesExpression n) {
        Boolean savedEval = eval;
        eval = true;
        n.f0.accept(this);
        String op1 = lastExpression;

        eval = true;
        n.f2.accept(this);
        eval = savedEval;
        String op2 = lastExpression;
        lastExpression = String.format("MulS(%s %s)", op1, op2);
    }

    @Override
    public void visit(ArrayLookup n) {
        local = true;
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
        reference = true;
        n.f0.accept(this);
        String objClass = this.objClass;
        String callInstance = lastExpression;
        lastExpression = "";
        reference = false;

        Boolean savedEval = eval;
        eval = true;
        n.f4.accept(this);
        eval = savedEval;
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
        if ((local || eval) && (lastExpression.contains("+") || newAlloc)) {
            print("t.%d = %s", varCount, lastExpression);
            lastExpression = String.format("t.%d", varCount++);
            if (newAlloc) {
                print("if %s goto :null%d", lastExpression, varCount, nullCount);
                indent++;
                print("Error(\"null pointer\")");
                indent--;
                print("null%d:", nullCount++);
                newAlloc = false;
            }
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
            if (reference && var.type == VarType.REFERENCE) {
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
            if (reference && var.type == VarType.REFERENCE) {
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
            newAlloc = true;
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

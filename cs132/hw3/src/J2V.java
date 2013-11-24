import syntaxtree.*;
import visitor.DepthFirstVisitor;

import java.util.ArrayList;

public class J2V extends DepthFirstVisitor {
    private boolean eval;
    private boolean ifNotWhile;

    public static void main(String[] args) {
        try {
            Node root = new MiniJavaParser(System.in).Goal();
            MySymbolTable table = new MySymbolTable();
            root.accept(table);
            root.accept(new J2V(table));
        } catch (ParseException e) {
            System.out.println(e.toString());
        }
    }

    J2V(MySymbolTable table) {
        this.table = table;
    }

    MySymbolTable table;

    int indent;
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
        System.out.println(String.format(ret + s, arg));
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
        print("");
        Boolean skip = true;
        for (MyType type : table.classTable.values()) {
            if (type != MyType.ARRAY && type != MyType.BOOLEAN && type != MyType.INTEGER) {
                if (!skip) {
                    ArrayList<String> overloadedMethods = new ArrayList<String>();
                    for (MyType.Method method : type.methods.values())
                        if (method.override)
                            overloadedMethods.add(method.name);
                    if (overloadedMethods.isEmpty()) {
                        print("const empty_%s", type.name);
                    } else {
                        print("const vmt_%s", type.name);
                        indent++;
                        for (String s : overloadedMethods)
                            print(":%s.%s", type.name, s);
                        indent--;
                    }
                    print("");
                }
                skip = false;
            }
        }

        print("");
        n.f0.accept(this);
        print("");
        n.f1.accept(this);
        print("");

        if (allocArray)
            printArrayAlloc();
    }

    @Override
    public void visit(MainClass n) {
        varCount = 0;
        print("func Main()");

        indent++;
        n.f15.accept(this);
        print("ret");
        indent--;
    }

    @Override
    public void visit(ClassDeclaration n) {
        table.setClassScope(n.f1.f0.tokenImage);
        n.f4.accept(this);
        table.clearClassScope();
    }

    @Override
    public void visit(ClassExtendsDeclaration n) {
        table.setClassScope(n.f1.f0.tokenImage);
        n.f6.accept(this);
        table.clearClassScope();
    }

    @Override
    public void visit(MethodDeclaration n) {
        varCount = 0;
        table.setMethodScope(n.f2.f0.tokenImage);
        print("func %s(%s)", table.getMethodScope(), table.classScope.getMethodArgsString(n.f2.f0.tokenImage));

        indent++;
        n.f8.accept(this);

        reference = false;
        lastExpression = "";
        n.f10.accept(this);
        print("ret %s", lastExpression);
        indent--;
        print("");
        table.clearMethodScope();
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
        lastExpression = n.f1.f0.tokenImage;
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
        n.f0.accept(this);
        String t1 = String.format("t.%d", varCount++);
        print("%s = %s", t1, lastExpression);

        int nullCount = this.nullCount++;
        print("if %s goto :null%d", t1, nullCount);
        indent++;
        print("Error(\"null pointer\")");
        indent--;

        print("null%d:", nullCount);

        String t2 = String.format("t.%d", this.varCount++);
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

        n.f5.accept(this);
        print("[%s+4] = %s", t2, lastExpression);
    }

    @Override
    public void visit(IfStatement n) {
        int ifCount = this.ifCount++;
        reference = true;
        not = false;
        local = true;
        ifNotWhile = true;

        n.f2.accept(this);
        ifNotWhile = false;
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

        not = false;
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
        if ((local || eval) && (lastExpression.contains(" ") || lastExpression.contains("+"))) {
            String t = String.format("t.%d", varCount++);
            print("%s = %s", t, lastExpression);
            lastExpression = t;
        }
    }

    @Override
    public void visit(AndExpression n) {
        Boolean savedEval = eval;
        eval = true;
        not = false;
        n.f0.accept(this);

        if (ifNotWhile) {
            print("if %s goto :if%d_else", lastExpression, ifCount - 1);

            eval = true;
            n.f2.accept(this);
            eval = savedEval;
        } else {
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
        lastExpression = String.format("[%s+4]", t2);
    }

    @Override
    public void visit(ArrayLength n) {
    }

    @Override
    public void visit(MessageSend n) {
        reference = true;
        local = true;
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
        newAlloc = false;
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
        objClass = table.classScope.name;
        lastExpression = objClass;
        Integer offset = table.classTable.get(lastExpression).memoryOffsets.get(n.f0.tokenImage);
        lastExpression = n.f0.tokenImage;

        if (objClass.equals(table.classScope.name))
            objClass = "this";
        if (offset != null)
            lastExpression = String.format("[%s+%d]", objClass, offset);

        MyType type = table.getVarType(lastExpression);
        if (type != null) {
            if (reference && type != MyType.ARRAY && type != MyType.BOOLEAN && type != MyType.INTEGER) {
                print("if %s goto :null%d", lastExpression, nullCount);
                indent++;
                print("Error(\"null pointer\")");
                indent--;
                print("null%d:", nullCount++);
            }
            objClass = type.name;
        }
    }

    @Override
    public void visit(ThisExpression n) {
        lastExpression = "this";
        objClass = table.classScope.name;
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
        int size = table.classTable.get(objClass).memoryOffsets.size() * 4;
        if (size != 0) {
            lastExpression = String.format("HeapAllocZ(%d)", size);
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

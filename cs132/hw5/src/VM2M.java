import cs132.util.IndentPrinter;
import cs132.util.ProblemException;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.parser.VaporParser;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;


public class VM2M extends VInstr.Visitor<Throwable> {

    private static IndentPrinter printer;
    private static boolean print;
    private static boolean error;
    private static boolean heapAlloc;
    private static boolean nullPointer;
    private static boolean array;

    public static void main(String[] args)
            throws Throwable {
        Op[] ops = {
                Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
                Op.PrintIntS, Op.HeapAllocZ, Op.Error,
        };
        boolean allowLocals = false;
        String[] registers = {
                "v0", "v1",
                "a0", "a1", "a2", "a3",
                "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
                "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
                "t8",
        };
        boolean allowStack = true;

        VaporProgram program = null;
        try {
            program = VaporParser.run(new InputStreamReader(System.in), 1, 1,
                    java.util.Arrays.asList(ops),
                    allowLocals, registers, allowStack);
        } catch (ProblemException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        printer = new IndentPrinter(new PrintWriter(System.out), "  ");
        printer.println(".data");
        printer.println("");
        for (VDataSegment segment : program.dataSegments) {
            printer.println(String.format("%s:", segment.ident));
            printer.indent();
            for (VOperand operand : segment.values)
                printer.println(((VLabelRef) operand).ident);
            printer.dedent();
            printer.println("");
        }

        printer.println(".text");
        printer.println("");
        printer.indent();
        printer.println("jal Main");
        printer.println("li $v0 10");
        printer.println("syscall");
        printer.dedent();
        printer.println("");

        VM2M v2vm = new VM2M();
        for (VFunction function : program.functions) {
            printer.println(String.format("%s:", function.ident));
            printer.indent();
            printer.println("sw $fp -8($sp)");
            printer.println("move $fp $sp");
            printer.println(String.format("subu $sp $sp %d", 4 * (2 + function.stack.out + function.stack.local)));
            printer.println("sw $ra -4($fp)");

            int line;
            LinkedList<VCodeLabel> labels = new LinkedList<VCodeLabel>();
            Collections.addAll(labels, function.labels);
            for (VInstr instr : function.body) {
                line = instr.sourcePos.line;
                printer.dedent();
                while (!labels.isEmpty() && labels.peek().sourcePos.line < line)
                    printer.println(String.format("%s:", labels.pop().ident));
                printer.indent();
                instr.accept(v2vm);
            }


            printer.println("lw $ra -4($fp)");
            printer.println("lw $fp -8($fp)");
            printer.println(String.format("addu $sp $sp %d", 4 * (2 + function.stack.out + function.stack.local)));
            printer.println("jr $ra");
            printer.dedent();
            printer.println("");
        }

        if (print) {
            printer.println("_print:");
            printer.indent();
            printer.println("li $v0 1   # syscall: print integer");
            printer.println("syscall");
            printer.println("la $a0 _newline");
            printer.println("li $v0 4   # syscall: print string");
            printer.println("syscall");
            printer.println("jr $ra");
            printer.dedent();
            printer.println("");
        }
        if (error) {
            printer.println("_error:");
            printer.indent();
            printer.println("li $v0 4   # syscall: print string");
            printer.println("syscall");
            printer.println("li $v0 10  # syscall: exit");
            printer.println("syscall");
            printer.dedent();
            printer.println("");
        }
        if (heapAlloc) {
            printer.println("_heapAlloc:");
            printer.indent();
            printer.println("li $v0 9   # syscall: sbrk");
            printer.println("syscall");
            printer.println("jr $ra");
            printer.dedent();
            printer.println("");
        }
        printer.println(".data");
        printer.println(".align 0");
        printer.println("_newline: .asciiz \"\\n\"");
        if (nullPointer)
            printer.println("_str0: .asciiz \"null pointer\\n\"");
        if (array)
            printer.println("_str1: .asciiz \"array index out of bounds\\n\"");

        printer.close();
    }

    @Override
    public void visit(VAssign vAssign) throws Throwable {
        if (vAssign.source instanceof VVarRef.Register)
            printer.println(String.format("move %s %s", vAssign.dest.toString(), vAssign.source.toString()));
        else if (vAssign.source instanceof VLabelRef)
            printer.println(String.format("la %s %s", vAssign.dest.toString(), ((VLabelRef) vAssign.source).ident));
        else
            printer.println(String.format("li %s %s", vAssign.dest.toString(), vAssign.source));
    }

    @Override
    public void visit(VCall vCall) throws Throwable {
        if (vCall.addr instanceof VAddr.Label) {
            printer.println(String.format("jal %s", ((VAddr.Label) vCall.addr).label.ident));
        } else {
            printer.println(String.format("jalr %s", vCall.addr));
        }
        if (vCall.dest != null)
            printer.println(String.format("%s = $v0", vCall.dest));
    }

    @Override
    public void visit(VBuiltIn vBuiltIn) throws Throwable {
        if (vBuiltIn.op.name.equals("Error")) {
            if (vBuiltIn.args[0].toString().equals("\"null pointer\"")) {
                printer.println("la $a0 _str0");
                nullPointer = true;
            } else {
                printer.println("la $a0 _str1");
                array = true;
            }
            printer.println("j _error");
            error = true;
        } else if (vBuiltIn.op.name.equals("HeapAllocZ") || vBuiltIn.op.name.equals("PrintIntS")) {
            for (int i = 0; i < vBuiltIn.args.length; i++) {
                if (vBuiltIn.args[i] instanceof VVarRef.Register)
                    printer.println(String.format("move $a%d %s", i, vBuiltIn.args[i].toString()));
                else
                    printer.println(String.format("li $a%d %s", i, vBuiltIn.args[i]));
            }

            if (vBuiltIn.op.name.equals("HeapAllocZ")) {
                printer.println(String.format("jal _heapAlloc"));
                heapAlloc = true;
            } else if (vBuiltIn.op.name.equals("PrintIntS")) {
                printer.println(String.format("jal _print"));
                print = true;
            }

            if (vBuiltIn.dest != null)
                printer.println(String.format("move %s $v0", vBuiltIn.dest));
        } else {
            String op = vBuiltIn.op.name;
            boolean arithmetic = false;
            if (op.equals("Lt")) {
                op = "sltu";
            } else if (op.equals("LtS")) {
                op = "slt";
            } else if (op.equals("Sub")) {
                op = "subu";
                arithmetic = true;
            } else if (op.equals("MulS")) {
                op = "mul";
                arithmetic = true;
            } else if (op.equals("Add")) {
                op = "addu";
                arithmetic = true;
            }
            if (arithmetic && vBuiltIn.args[0] instanceof VLitInt && vBuiltIn.args[1] instanceof VLitInt) {
                if (op.equals("subu"))
                    printer.println(String.format("li %s %d", vBuiltIn.dest,
                            Integer.parseInt(vBuiltIn.args[0].toString()) -
                                    Integer.parseInt(vBuiltIn.args[1].toString())));
                if (op.equals("mul"))
                    printer.println(String.format("li %s %d", vBuiltIn.dest,
                            Integer.parseInt(vBuiltIn.args[0].toString()) *
                                    Integer.parseInt(vBuiltIn.args[1].toString())));
                if (op.equals("addu"))
                    printer.println(String.format("li %s %d", vBuiltIn.dest,
                            Integer.parseInt(vBuiltIn.args[0].toString()) +
                                    Integer.parseInt(vBuiltIn.args[1].toString())));
            } else if (vBuiltIn.args[0] instanceof VLitInt) {
                printer.println(String.format("li $t9 %s", vBuiltIn.args[0]));
                printer.println(String.format("%s %s $t9 %s", op, vBuiltIn.dest, vBuiltIn.args[1]));
            } else {
                printer.println(String.format("%s %s %s %s", op, vBuiltIn.dest, vBuiltIn.args[0], vBuiltIn.args[1]));
            }
        }
    }

    @Override
    public void visit(VMemWrite vMemWrite) throws Throwable {
        if (vMemWrite.dest instanceof VMemRef.Global) {
            VMemRef.Global dest = (VMemRef.Global) vMemWrite.dest;
            if (vMemWrite.source instanceof VLabelRef) {
                printer.println(String.format("la $t9 %s", ((VLabelRef) vMemWrite.source).ident));
                printer.println(String.format("sw $t9 %d(%s)", dest.byteOffset, dest.base));
            }
            printer.println(String.format("sw %s %d(%s)", vMemWrite.source, dest.byteOffset, dest.base));
        } else {
            VMemRef.Stack dest = (VMemRef.Stack) vMemWrite.dest;
            if (vMemWrite.source instanceof VVarRef.Register) {
                printer.println(String.format("sw %s %d($sp)", vMemWrite.source, 4 * dest.index));
            } else {
                printer.println(String.format("li $t9 %s", vMemWrite.source));
                printer.println(String.format("sw $t9 %d($sp)", 4 * dest.index));
            }
        }
    }

    @Override
    public void visit(VMemRead vMemRead) throws Throwable {
        String source = "$sp";
        int byteOffset;
        if (vMemRead.source instanceof VMemRef.Global) {
            source = ((VMemRef.Global) vMemRead.source).base.toString();
            byteOffset = ((VMemRef.Global) vMemRead.source).byteOffset;
        } else {
            byteOffset = 4 * ((VMemRef.Stack) vMemRead.source).index;
            if (((VMemRef.Stack) vMemRead.source).region == VMemRef.Stack.Region.In)
                source = "$fp";
        }
        printer.println(String.format("lw %s %d(%s)", vMemRead.dest.toString(), byteOffset, source));
    }

    @Override
    public void visit(VBranch vBranch) throws Throwable {
        if (vBranch.positive)
            printer.println(String.format("bnez %s %s", vBranch.value, vBranch.target.ident));
        else
            printer.println(String.format("beqz %s %s", vBranch.value, vBranch.target.ident));
    }

    @Override
    public void visit(VGoto vGoto) throws Throwable {
        printer.println(String.format("j %s", ((VAddr.Label) vGoto.target).label.ident));
    }

    @Override
    public void visit(VReturn vReturn) throws Throwable {
        if (vReturn.value != null) {
            String value = vReturn.value.toString();
            if (value == null)
                value = vReturn.value.toString();
            printer.println(String.format("$v0 = %s", value));
        }
    }
}

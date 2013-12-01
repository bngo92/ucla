import cs132.util.IndentPrinter;
import cs132.util.ProblemException;
import cs132.util.StringUtil;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.parser.VaporParser;

import java.io.*;
import java.util.*;


public class V2VM extends VInstr.Visitor<Throwable> {

    private static IndentPrinter printer;
    private static LinkedHashMap<String,String> registerMap;

    public static void main(String[] args)
            throws Throwable {
        Op[] ops = {
                Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
                Op.PrintIntS, Op.HeapAllocZ, Op.Error,
        };
        boolean allowLocals = true;
        String[] registers = null;
        boolean allowStack = false;

        VaporProgram program = null;
        try {
            program = VaporParser.run(new InputStreamReader(new FileInputStream("Factorial.vapor")), 1, 1,
                    java.util.Arrays.asList(ops),
                    allowLocals, registers, allowStack);
        } catch (ProblemException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        printer = new IndentPrinter(new PrintWriter(System.out), "  ");
        for (VDataSegment segment : program.dataSegments) {
            printer.println(String.format("const %s", segment.ident));
            printer.indent();
            for (VOperand operand : segment.values)
                printer.println(String.format(":%s", ((VLabelRef) operand).ident));
            printer.dedent();
            printer.println("");
        }

        for (VFunction function : program.functions) {
            LinkedList<VCodeLabel> labels = new LinkedList<VCodeLabel>();
            Collections.addAll(labels, function.labels);

            printer.println(String.format("func %s [in %d, out %d, local %d]", function.ident, function.stack.in, function.stack.out, function.stack.local));
            printer.indent();

            Liveness liveness = new Liveness();
            for (VVarRef varRef : function.params) {
                String var = varRef.toString();
                liveness.things.put(var, new Liveness.Thing(var, varRef.sourcePos.line));
            }

            for (VInstr instr : function.body)
                instr.accept(liveness);

            registerMap = new LinkedHashMap<String, String>();
            HashMap<String, Liveness.Thing> registerMapBuilder = new HashMap<String, Liveness.Thing>();
            int last = 0;
            for (Liveness.Thing thing : liveness.things.values()) {
                String register = String.format("$t%d", last);
                Liveness.Thing saved = registerMapBuilder.get(register);
                if (saved == null || thing.range.start >= saved.range.end) {
                    registerMap.put(thing.var, register);
                    registerMapBuilder.put(register, thing);
                    continue;
                }

                for (int i = 0; i < 9; i++) {
                    register = String.format("$t%d", i);
                    saved = registerMapBuilder.get(register);
                    if (saved == null || thing.range.start >= saved.range.end) {
                        registerMap.put(thing.var, register);
                        registerMapBuilder.put(register, thing);
                        last = i;
                        break;
                    }
                }
            }

            for (int i = 0; i < function.params.length; i++) {
                printer.println(String.format("%s = $a%d", registerMap.get(function.params[i].toString()), i));
            }

            int line;
            for (VInstr instr : function.body) {
                line = instr.sourcePos.line;
                if (!labels.isEmpty() && labels.peek().sourcePos.line < line) {
                    printer.dedent();
                    printer.println(String.format("%s:", labels.pop().ident));
                    printer.indent();
                }
                instr.accept(new V2VM());
            }

            printer.dedent();
            printer.println("");
        }

        printer.close();
    }

    @Override
    public void visit(VAssign vAssign) throws Throwable {
        printer.println(String.format("%s = %s", registerMap.get(vAssign.dest.toString()), registerMap.get(vAssign.source.toString())));
    }

    @Override
    public void visit(VCall vCall) throws Throwable {
        for (int i = 0; i < vCall.args.length; i++) {
            String arg = String.format("$a%d", i);
            String register = registerMap.get(vCall.args[i].toString());
            if (register == null)
                register = vCall.args[i].toString();
            printer.println(String.format("%s = %s", arg, register));
        }
        printer.println(String.format("call %s", registerMap.get(vCall.addr.toString())));
        printer.println(String.format("%s = $v0", registerMap.get(vCall.dest.toString())));
    }

    @Override
    public void visit(VBuiltIn vBuiltIn) throws Throwable {
        ArrayList<String> args = new ArrayList<String>();
        for (VOperand operand : vBuiltIn.args) {
            String register = registerMap.get(operand.toString());
            if (register == null)
                args.add(operand.toString());
            else
                args.add(register);
        }

        if (vBuiltIn.dest != null)
            printer.println(String.format("%s = %s(%s)", registerMap.get(vBuiltIn.dest.toString()), vBuiltIn.op.name, StringUtil.join(args, " ")));
        else
            printer.println(String.format("%s(%s)", vBuiltIn.op.name, StringUtil.join(args, " ")));
    }

    @Override
    public void visit(VMemWrite vMemWrite) throws Throwable {
        printer.println(String.format("[%s] = %s", registerMap.get(((VMemRef.Global) vMemWrite.dest).base.toString()), vMemWrite.source.toString()));
    }

    @Override
    public void visit(VMemRead vMemRead) throws Throwable {
        printer.println(String.format("%s = [%s]", registerMap.get(vMemRead.dest.toString()), registerMap.get(((VMemRef.Global) vMemRead.source).base.toString())));
    }

    @Override
    public void visit(VBranch vBranch) throws Throwable {
        printer.println(String.format("if %s goto %s", registerMap.get(vBranch.value.toString()), vBranch.target));
    }

    @Override
    public void visit(VGoto vGoto) throws Throwable {
        printer.println(String.format("goto %s", vGoto.target.toString()));
    }

    @Override
    public void visit(VReturn vReturn) throws Throwable {
        if (vReturn.value != null)
            printer.println(String.format("$v0 = %s", registerMap.get(vReturn.value.toString())));
        printer.println("ret");
    }
}

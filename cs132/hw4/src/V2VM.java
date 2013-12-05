import cs132.util.IndentPrinter;
import cs132.util.ProblemException;
import cs132.util.StringUtil;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VBuiltIn.Op;
import cs132.vapor.parser.VaporParser;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;


public class V2VM extends VInstr.Visitor<Throwable> {

    private static IndentPrinter printer;
    private static HashMap<String, String> registerMap;
    private static int locals;

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
            program = VaporParser.run(new InputStreamReader(System.in), 1, 1,
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

        V2VM v2vm = new V2VM();
        for (VFunction function : program.functions) {
            LivenessAnalysis livenessAnalysis = new LivenessAnalysis(function);
            livenessAnalysis.analyze();
            registerMap = livenessAnalysis.getRegisters();

            int in = function.params.length;
            locals = livenessAnalysis.calleeRegisterCount;
            printer.println(String.format("func %s [in %d, out %d, local %d]", function.ident, (in < 4) ? 0 : in - 4,
                    livenessAnalysis.out, livenessAnalysis.calleeRegisterCount));
            printer.indent();

            for (int i = 0; i < locals && i < 8; i++)
                printer.println(String.format("local[%d] = $s%d", i, i));

            for (int i = 0; i < function.params.length; i++) {
                String register = registerMap.get(function.params[i].toString());
                if (register == null)
                    continue;
                else if (i < 4)
                    printer.println(String.format("%s = $a%d", register, i));
                else
                    printer.println(String.format("%s = in[%d]", register, i - 4));
            }

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

            printer.dedent();
            printer.println("");
        }

        printer.close();
    }

    @Override
    public void visit(VAssign vAssign) throws Throwable {
        String source = registerMap.get(vAssign.source.toString());
        if (source != null)
            printer.println(String.format("%s = %s", registerMap.get(vAssign.dest.toString()), source));
    }

    @Override
    public void visit(VCall vCall) throws Throwable {
        for (int i = 0; i < vCall.args.length; i++) {
            String rhs = registerMap.get(vCall.args[i].toString());
            if (rhs == null)
                rhs = vCall.args[i].toString();

            if (i < 4)
                printer.println(String.format("$a%d = %s", i, rhs));
            else
                printer.println(String.format("out[%d] = %s", i - 4, rhs));
        }

        String addr = registerMap.get(vCall.addr.toString());
        if (addr == null)
            addr = vCall.addr.toString();
        printer.println(String.format("call %s", addr));
        String ret = registerMap.get(vCall.dest.toString());
        if (ret != null)
            printer.println(String.format("%s = $v0", ret));
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
        String source = registerMap.get(vMemWrite.source.toString());
        if (source == null)
            source = vMemWrite.source.toString();
        VMemRef.Global dest = (VMemRef.Global) vMemWrite.dest;
        if (dest.byteOffset == 0)
            printer.println(String.format("[%s] = %s", registerMap.get(dest.base.toString()), source));
        else
            printer.println(String.format("[%s+%d] = %s", registerMap.get(dest.base.toString()), dest.byteOffset, source));
    }

    @Override
    public void visit(VMemRead vMemRead) throws Throwable {
        VMemRef.Global source = (VMemRef.Global) vMemRead.source;
        if (source.byteOffset == 0)
            printer.println(String.format("%s = [%s]", registerMap.get(vMemRead.dest.toString()), registerMap.get(source.base.toString())));
        else
            printer.println(String.format("%s = [%s+%d]", registerMap.get(vMemRead.dest.toString()), registerMap.get(source.base.toString()), source.byteOffset));
    }

    @Override
    public void visit(VBranch vBranch) throws Throwable {
        if (vBranch.positive)
            printer.println(String.format("if %s goto %s", registerMap.get(vBranch.value.toString()), vBranch.target));
        else
            printer.println(String.format("if0 %s goto %s", registerMap.get(vBranch.value.toString()), vBranch.target));
    }

    @Override
    public void visit(VGoto vGoto) throws Throwable {
        printer.println(String.format("goto %s", vGoto.target.toString()));
    }

    @Override
    public void visit(VReturn vReturn) throws Throwable {
        if (vReturn.value != null) {
            String value = registerMap.get(vReturn.value.toString());
            if (value == null)
                value = vReturn.value.toString();
            printer.println(String.format("$v0 = %s", value));
        }

        for (int i = 0; i < locals && i < 8; i++)
            printer.println(String.format("$s%d = local[%d]", i, i));
        printer.println("ret");
    }
}

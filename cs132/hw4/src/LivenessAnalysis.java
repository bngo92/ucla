import cs132.vapor.ast.*;

import java.util.*;

public class LivenessAnalysis extends VInstr.Visitor<Throwable> {

    private final LinkedHashMap<String, VarRef> varRefs;
    private final VFunction function;
    public int out;
    public int s;
    private String label;

    public LivenessAnalysis(VFunction function) throws Throwable {
        this.function = function;
        this.label = null;
        this.varRefs = new LinkedHashMap<String, VarRef>();
    }

    public void analyze() throws Throwable {
        for (VVarRef varRef : function.params) {
            String var = varRef.toString();
            varRefs.put(var, new VarRef(var, varRef.sourcePos.line));
        }

        int line;
        LinkedList<VCodeLabel> labels = new LinkedList<VCodeLabel>();
        Collections.addAll(labels, function.labels);
        for (VInstr instr : function.body) {
            line = instr.sourcePos.line;
            while (!labels.isEmpty() && labels.peek().sourcePos.line < line) {
                String label = labels.pop().ident;
                if (!label.contains("bounds") && !label.contains("null"))
                    this.label = label;
            }
            instr.accept(this);
        }
    }

    public void crossCall() throws Throwable {
        CrossCall call = new CrossCall(varRefs);
        for (VInstr instr : function.body)
            instr.accept(call);
    }

    public LinkedHashMap<String, String> getRegisterMap() {
        LinkedHashMap<String,String> registerMap = new LinkedHashMap<String, String>();
        HashMap<String, VarRef> registerMapBuilder = new HashMap<String, VarRef>();
        int last = 0;
        s = 0;
        for (LivenessAnalysis.VarRef varRef : varRefs.values()) {
            if (varRef.crossCall) {
                String register = String.format("$s%d", s++);
                registerMap.put(varRef.var, register);
                registerMapBuilder.put(register, varRef);
                continue;
            }

            String register;
            if (last < 9) {
                register = String.format("$t%d", last);
            } else {
                register = String.format("$s%d", last - 9);
            }
            LivenessAnalysis.VarRef saved = registerMapBuilder.get(register);
            if (saved == null || varRef.range.start >= saved.range.end) {
                registerMap.put(varRef.var, register);
                registerMapBuilder.put(register, varRef);
                continue;
            }

            for (int i = 0; i < 17; i++) {
                if (i < 9) {
                    register = String.format("$t%d", i);
                    saved = registerMapBuilder.get(register);
                    if (saved == null || varRef.range.start >= saved.range.end) {
                        registerMap.put(varRef.var, register);
                        registerMapBuilder.put(register, varRef);
                        last = i;
                        break;
                    }
                } else {
                    register = String.format("$s%d", i - 9);
                    saved = registerMapBuilder.get(register);
                    if (saved == null || varRef.range.start >= saved.range.end) {
                        registerMap.put(varRef.var, register);
                        registerMapBuilder.put(register, varRef);
                        last = i;
                        break;
                    }
                }
            }
        }
        return registerMap;
    }

    private boolean isVar(VOperand operand) {
        return operand instanceof VVarRef.Local;
    }

    private boolean isVar(VMemRef vMemRef) {
        return vMemRef instanceof VMemRef.Global;
    }

    @Override
    public void visit(VAssign vAssign) throws Throwable {
        int line = vAssign.sourcePos.line;

        if (isVar(vAssign.source)) {
            String in = vAssign.source.toString();
            VarRef varRef = varRefs.get(in);
            varRef.range.end = line;
            if (label != null)
                varRef.labels.add(label);
        }

        String out = vAssign.dest.toString();
        VarRef varRef = varRefs.get(out);
        if (varRef == null) {
            varRef = new VarRef(out, line);
            varRefs.put(out, varRef);
        } else {
            varRef.range.end = line;
        }
    }

    @Override
    public void visit(VCall vCall) throws Throwable {
        int line = vCall.sourcePos.line;
        String out = vCall.dest.ident;
        boolean coalesce = false;

        for (VOperand operand : vCall.args) {
            if (!(isVar(operand)))
                continue;

            String in = operand.toString();
            VarRef varRef = varRefs.get(in);
            if (out.equals(in))
                coalesce = true;

            varRef.range.end = line;
            if (label != null)
                varRef.labels.add(label);
        }

        String in = vCall.addr.toString();
        VarRef varRef = varRefs.get(in);
        if (varRef != null) {
            if (out.equals(in))
                coalesce = true;
            varRef.range.end = line;
        }

        if (!coalesce) {
            varRef = varRefs.get(out);
            if (varRef == null) {
                varRef = new VarRef(out, line);
                varRefs.put(out, varRef);
            } else {
                varRef.range.end = line;
            }
        }

        if (vCall.args.length - 4 > this.out)
            this.out = vCall.args.length - 4;
    }

    @Override
    public void visit(VBuiltIn vBuiltIn) throws Throwable {
        int line = vBuiltIn.sourcePos.line;
        String out;
        boolean coalesce;

        if (vBuiltIn.dest != null) {
            out = vBuiltIn.dest.toString();
            coalesce = false;
        } else {
            out = "";
            coalesce = true;
        }

        for (VOperand operand : vBuiltIn.args) {
            if (!isVar(operand))
                continue;

            String in = operand.toString();
            VarRef varRef = varRefs.get(in);
            if (in.equals(out))
                coalesce = true;

            varRef.range.end = line;
            if (label != null)
                varRef.labels.add(label);
        }

        if (!coalesce) {
            VarRef varRef = varRefs.get(out);
            if (varRef == null) {
                varRef = new VarRef(out, line);
                varRefs.put(out, varRef);
            } else {
                varRef.range.end = line;
            }
        }
    }

    @Override
    public void visit(VMemWrite vMemWrite) throws Throwable {
        int line = vMemWrite.sourcePos.line;

        if (isVar(vMemWrite.source)) {
            String in = vMemWrite.source.toString();
            VarRef varRef = varRefs.get(in);
            varRef.range.end = line;
            if (label != null)
                varRef.labels.add(label);
        }

        String out;
        if (isVar(vMemWrite.dest))
            out = ((VMemRef.Global) vMemWrite.dest).base.toString();
        else
            throw new Throwable();

        VarRef varRef = varRefs.get(out);
        varRef.range.end = line;
        if (label != null)
            varRef.labels.add(label);
    }

    @Override
    public void visit(VMemRead vMemRead) throws Throwable {
        int line = vMemRead.sourcePos.line;

        if (isVar(vMemRead.source)) {
            String in = ((VMemRef.Global) vMemRead.source).base.toString();
            VarRef varRef = varRefs.get(in);
            varRef.range.end = line;
            if (label != null)
                varRef.labels.add(label);
        }

        if (isVar(vMemRead.dest)) {
            String out = vMemRead.dest.toString();
            VarRef varRef = varRefs.get(out);
            if (varRef == null) {
                varRef = new VarRef(out, line);
                varRefs.put(out, varRef);
            } else {
                varRef.range.end = line;
            }
        }
    }

    @Override
    public void visit(VBranch vBranch) throws Throwable {
        for (VarRef varRef : varRefs.values()) {
            if (varRef.labels.contains(vBranch.target.toString().substring(1)))
                varRef.range.end = vBranch.sourcePos.line;
        }

        int line = vBranch.sourcePos.line;

        String in = vBranch.value.toString();
        VarRef varRef = varRefs.get(in);
        varRef.range.end = line;
    }

    @Override
    public void visit(VGoto vGoto) throws Throwable {
        for (VarRef varRef : varRefs.values())
            if (varRef.labels.contains(vGoto.target.toString().substring(1)))
                varRef.range.end = vGoto.sourcePos.line;
    }

    @Override
    public void visit(VReturn vReturn) throws Throwable {
        int line = vReturn.sourcePos.line;

        if (isVar(vReturn.value)) {
            String in = vReturn.value.toString();
            VarRef varRef = varRefs.get(in);
            varRef.range.end = line;
        }
    }

    public static class Range {
        public final int start;
        public int end;
        public Range(int start) {
            this.start = start;
            this.end = start;
        }
    }

    public static class VarRef {
        public final String var;
        public final Range range;
        public final HashSet<String> labels;
        public boolean crossCall;
        public VarRef(String var, int start) {
            this.var = var;
            this.range = new Range(start);
            labels = new HashSet<String>();
        }
    }
}

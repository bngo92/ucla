import cs132.vapor.ast.*;

import java.util.*;

public class LivenessAnalysis extends VInstr.Visitor<Throwable> {

    private final LinkedHashMap<String, VarRef> varRefs;
    private final VFunction function;
    public int out;
    public int calleeRegisterCount;
    private String label;
    private static HashMap<VarRef, String> registers = new HashMap<VarRef, String>();
    private static LinkedList<String> calleeRegisters = new LinkedList<String>();
    private static LinkedList<String> callerRegisters = new LinkedList<String>();
    private static LinkedHashSet<String> freeRegisters = new LinkedHashSet<String>();
    static {
        calleeRegisters.addAll(Arrays.asList("$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"));
        callerRegisters.addAll(Arrays.asList("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8"));
    }

    private TreeSet<VarRef> active;
    private HashMap<VarRef, Integer> locations;

    public LivenessAnalysis(VFunction function) throws Throwable {
        this.function = function;
        this.label = null;
        this.varRefs = new LinkedHashMap<String, VarRef>();
    }

    public HashMap<String, String> getRegisters() {
        allocateRegisters();
        HashMap<String, String> ret = new HashMap<String, String>();
        for (Map.Entry<VarRef, String> entry : registers.entrySet())
            ret.put(entry.getKey().var, entry.getValue());
        return ret;
    }

    private String getFreeRegister(boolean callee) {
        String ret;
        if (callee) {
            Iterator<String> iterator = freeRegisters.iterator();
            while (iterator.hasNext()) {
                ret = iterator.next();
                if (ret.charAt(1) == 's') {
                    iterator.remove();
                    return ret;
                }
            }

            calleeRegisterCount++;
            ret = calleeRegisters.getFirst();
            callerRegisters.removeFirst();
            return ret;
        }

        Iterator<String> iterator = freeRegisters.iterator();
        if (iterator.hasNext()) {
            ret = iterator.next();
            freeRegisters.remove(ret);
        } else {
            if (callerRegisters.isEmpty()) {
                calleeRegisterCount++;
                ret = calleeRegisters.getFirst();
                calleeRegisters.removeFirst();
            } else {
                ret = callerRegisters.getFirst();
                callerRegisters.removeFirst();
            }
        }
        return ret;
    }

    private void allocateRegisters() {
        TreeSet<VarRef> liveIntervals = new TreeSet<VarRef>(new SortStart());
        for (VarRef varRef : varRefs.values())
            liveIntervals.add(varRef);

        active = new TreeSet<VarRef>(new SortEnd());
        for (VarRef varRef : liveIntervals) {
            expireOldIntervals(varRef);
            if (active.size() == 17) {
                spillAtInterval(varRef);
            } else {
                registers.put(varRef, getFreeRegister(varRef.crossCall));
                active.add(varRef);
            }
        }
    }

    private void expireOldIntervals(VarRef i) {
        Iterator<VarRef> iterator = active.iterator();
        while (iterator.hasNext()) {
            VarRef j = iterator.next();
            if (j.range.end >= i.range.start)
                return;
            iterator.remove();
            freeRegisters.add(registers.get(j));
        }
    }

    private void spillAtInterval(VarRef i) {
        VarRef spill = active.last();
        if (spill.range.end > i.range.end) {
            registers.put(i, registers.get(spill));
            locations.put(spill, calleeRegisterCount++);
            active.remove(spill);
            active.add(i);
        } else {
            locations.put(i, calleeRegisterCount++);
        }
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
        calleeRegisterCount = 0;
        for (LivenessAnalysis.VarRef varRef : varRefs.values()) {
            if (varRef.crossCall) {
                String register = String.format("$s%d", calleeRegisterCount++);
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

    public class SortStart implements Comparator<VarRef> {
        public int compare(VarRef r1, VarRef r2) {
            if (r1.range.start < r2.range.start)
                return -1;
            else if (r1.range.start > r2.range.start)
                return 1;
            else
                return 0;
        }
    }

    public class SortEnd implements Comparator<VarRef> {
        public int compare(VarRef r1, VarRef r2) {
            if (r1.range.end < r2.range.end)
                return -1;
            else if (r1.range.end > r2.range.end)
                return 1;
            else
                return 0;
        }
    }
}

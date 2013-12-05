import cs132.vapor.ast.*;

import java.util.*;

public class LivenessAnalysis extends VInstr.Visitor<Throwable> {

    private final LinkedHashMap<String, VarRef> varRefs = new LinkedHashMap<String, VarRef>();
    private final VFunction function;
    public int out;
    public int calleeRegisterCount;
    private HashMap<VarRef, String> registers = new HashMap<VarRef, String>();
    private LinkedList<String> calleeRegisters = new LinkedList<String>();
    private LinkedList<String> callerRegisters = new LinkedList<String>();
    private LinkedList<String> freeRegisters = new LinkedList<String>();

    private LinkedList<VarRef> active;
    private HashMap<VarRef, String> locations = new HashMap<VarRef, String>();

    public LivenessAnalysis(VFunction function) throws Throwable {
        this.function = function;
        calleeRegisters.addAll(Arrays.asList("$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"));
        callerRegisters.addAll(Arrays.asList("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8"));
    }

    public HashMap<String, String> getRegisters() {
        allocateRegisters();
        HashMap<String, String> ret = new HashMap<String, String>();
        for (Map.Entry<VarRef, String> entry : registers.entrySet())
            ret.put(entry.getKey().var, entry.getValue());
        for (Map.Entry<VarRef, String> entry : locations.entrySet())
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
            calleeRegisters.removeFirst();
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
        active = new LinkedList<VarRef>();
        LinkedList<VarRef> liveIntervals = new LinkedList<VarRef>(varRefs.values());
        Collections.sort(liveIntervals, new SortStart());
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
        Collections.sort(active, new SortEnd());
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
        VarRef spill = active.getLast();
        if (spill.range.end > i.range.end) {
            registers.put(i, registers.get(spill));
            locations.put(spill, String.format("local[%d]", calleeRegisterCount++));
            active.remove(spill);
            active.add(i);
            Collections.sort(active, new SortEnd());
        } else {
            locations.put(i, String.format("local[%d]", calleeRegisterCount++));
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
                for (VarRef varRef : varRefs.values())
                    varRef.labels.add(label);
            }
            instr.accept(this);
        }
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
            varRef.read(line);
        }

        String out = vAssign.dest.toString();
        VarRef varRef = varRefs.get(out);
        if (varRef == null) {
            varRef = new VarRef(out, line);
            varRefs.put(out, varRef);
        } else {
            varRef.write(line);
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
            varRef.read(line);
        }

        String in = vCall.addr.toString();
        VarRef varRef = varRefs.get(in);
        if (varRef != null) {
            if (out.equals(in))
                coalesce = true;
            varRef.read(line);
        }

        for (VarRef v : varRefs.values())
            v.call = true;

        if (!coalesce) {
            varRef = varRefs.get(out);
            if (varRef == null) {
                varRef = new VarRef(out, line);
                varRefs.put(out, varRef);
            } else {
                varRef.read(line);
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
            varRef.read(line);
        }

        if (!coalesce) {
            VarRef varRef = varRefs.get(out);
            if (varRef == null) {
                varRef = new VarRef(out, line);
                varRefs.put(out, varRef);
            } else {
                varRef.write(line);
            }
        }
    }

    @Override
    public void visit(VMemWrite vMemWrite) throws Throwable {
        int line = vMemWrite.sourcePos.line;

        if (isVar(vMemWrite.source)) {
            String in = vMemWrite.source.toString();
            VarRef varRef = varRefs.get(in);
            varRef.read(line);
        }

        String out;
        if (isVar(vMemWrite.dest))
            out = ((VMemRef.Global) vMemWrite.dest).base.toString();
        else
            throw new Throwable();

        VarRef varRef = varRefs.get(out);
        varRef.read(line);
    }

    @Override
    public void visit(VMemRead vMemRead) throws Throwable {
        int line = vMemRead.sourcePos.line;

        if (isVar(vMemRead.source)) {
            String in = ((VMemRef.Global) vMemRead.source).base.toString();
            VarRef varRef = varRefs.get(in);
            varRef.read(line);
        }

        if (isVar(vMemRead.dest)) {
            String out = vMemRead.dest.toString();
            VarRef varRef = varRefs.get(out);
            if (varRef == null) {
                varRef = new VarRef(out, line);
                varRefs.put(out, varRef);
            } else {
                varRef.write(line);
            }
        }
    }

    @Override
    public void visit(VBranch vBranch) throws Throwable {
        for (VarRef varRef : varRefs.values()) {
            if (varRef.readLabels.contains(vBranch.target.toString().substring(1)))
                varRef.range.end = vBranch.sourcePos.line;
        }

        int line = vBranch.sourcePos.line;

        String in = vBranch.value.toString();
        VarRef varRef = varRefs.get(in);
        varRef.read(line);
    }

    @Override
    public void visit(VGoto vGoto) throws Throwable {
        for (VarRef varRef : varRefs.values())
            if (varRef.readLabels.contains(vGoto.target.toString().substring(1)))
                varRef.range.end = vGoto.sourcePos.line;
    }

    @Override
    public void visit(VReturn vReturn) throws Throwable {
        int line = vReturn.sourcePos.line;

        if (isVar(vReturn.value)) {
            String in = vReturn.value.toString();
            VarRef varRef = varRefs.get(in);
            varRef.read(line);
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
        public final HashSet<String> readLabels;
        public boolean call;
        public boolean crossCall;
        public VarRef(String var, int start) {
            this.var = var;
            this.range = new Range(start);
            this.labels = new HashSet<String>();
            this.readLabels = new HashSet<String>();
            this.call = false;
            this.crossCall = false;
        }
        public void read(int line) {
            range.end = line;
            readLabels.addAll(labels);
            labels.clear();
            if (call)
                crossCall = true;
        }
        public void write(int line) {
            range.end = line;
            labels.clear();
            call = false;
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

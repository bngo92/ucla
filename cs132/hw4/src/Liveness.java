import cs132.vapor.ast.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Liveness extends VInstr.Visitor<Throwable> {

    public static class Range {
        public Range() {}
        public Range(int start) {
            this.start = start;
            this.end = -1;
        }
        public int start;
        public int end;
    }

    public static class Thing {
        public String var;
        public final Range range;
        public final LinkedList<Range> ranges;
        public boolean crossCall;
        public final HashSet<String> labels;
        public Thing(String var, int start) {
            this.var = var;
            range = new Range();
            ranges = new LinkedList<Range>();
            ranges.add(new Range(start));
            labels = new HashSet<String>();
        }
    }

    public int out;
    public String label;
    public final LinkedHashMap<String, Thing> things;

    public Liveness() {
        label = null;
        things = new LinkedHashMap<String, Thing>();
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
            Thing thing = things.get(in);
            thing.ranges.getLast().end = line;
            if (label != null)
                thing.labels.add(label);
        }

        String out = vAssign.dest.toString();
        Thing thing = things.get(out);
        if (thing == null) {
            thing = new Thing(out, line);
            things.put(out, thing);
        } else {
            thing.ranges.push(new Range(line));
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
            Thing thing = things.get(in);
            if (out.equals(in))
                coalesce = true;

            thing.ranges.getLast().end = line;
            if (label != null)
                thing.labels.add(label);
        }

        String in = vCall.addr.toString();
        Thing thing = things.get(in);
        if (thing != null) {
            if (out.equals(in))
                coalesce = true;
            thing.ranges.getLast().end = line;
        }

        if (!coalesce) {
            thing = things.get(out);
            if (thing == null) {
                thing = new Thing(out, line);
                things.put(out, thing);
            } else {
                thing.ranges.push(new Range(line));
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
            Thing thing = things.get(in);
            if (in.equals(out))
                coalesce = true;

            thing.ranges.getLast().end = line;
            if (label != null)
                thing.labels.add(label);
        }

        if (!coalesce) {
            Thing thing = things.get(out);
            if (thing == null) {
                thing = new Thing(out, line);
                things.put(out, thing);
            } else {
                thing.ranges.push(new Range(line));
            }
        }
    }

    @Override
    public void visit(VMemWrite vMemWrite) throws Throwable {
        int line = vMemWrite.sourcePos.line;

        if (isVar(vMemWrite.source)) {
            String in = vMemWrite.source.toString();
            Thing thing = things.get(in);
            thing.ranges.getLast().end = line;
            if (label != null)
                thing.labels.add(label);
        }

        String out;
        if (isVar(vMemWrite.dest))
            out = ((VMemRef.Global) vMemWrite.dest).base.toString();
        else
            throw new Throwable();

        Thing thing = things.get(out);
        thing.ranges.push(new Range(line));
        if (label != null)
            thing.labels.add(label);
    }

    @Override
    public void visit(VMemRead vMemRead) throws Throwable {
        int line = vMemRead.sourcePos.line;

        if (isVar(vMemRead.source)) {
            String in = ((VMemRef.Global) vMemRead.source).base.toString();
            Thing thing = things.get(in);
            thing.ranges.getLast().end = line;
            if (label != null)
                thing.labels.add(label);
        }

        if (isVar(vMemRead.dest)) {
            String out = vMemRead.dest.toString();
            Thing thing = things.get(out);
            if (thing == null) {
                thing = new Thing(out, line);
                things.put(out, thing);
            } else {
                thing.ranges.push(new Range(line));
            }
        }
    }

    @Override
    public void visit(VBranch vBranch) throws Throwable {
        for (Thing thing : things.values())
            if (thing.labels.contains(vBranch.target.toString().substring(1)))
                thing.ranges.getLast().end = vBranch.sourcePos.line;

        int line = vBranch.sourcePos.line;

        String in = vBranch.value.toString();
        Thing thing = things.get(in);
        thing.ranges.getLast().end = line;
    }

    @Override
    public void visit(VGoto vGoto) throws Throwable {
        for (Thing thing : things.values())
            if (thing.labels.contains(vGoto.target.toString().substring(1)))
                thing.ranges.getLast().end = vGoto.sourcePos.line;
    }

    @Override
    public void visit(VReturn vReturn) throws Throwable {
        int line = vReturn.sourcePos.line;

        if (isVar(vReturn.value)) {
            String in = vReturn.value.toString();
            Thing thing = things.get(in);
            thing.ranges.getLast().end = line;
        }
    }
}

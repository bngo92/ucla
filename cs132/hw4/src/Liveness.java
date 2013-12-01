import cs132.vapor.ast.*;

import java.util.LinkedHashMap;

public class Liveness extends VInstr.Visitor<Throwable> {

    public static class Range {
        public Range(int start) {
            this.start = start;
            this.end = start;
        }
        public final int start;
        public int end;
    }

    public static class Thing {
        public final String var;
        public final Range range;
        public boolean crossCall;
        public Thing(String var, int start) {
            this.var = var;
            this.range = new Range(start);
        }
    }

    public final LinkedHashMap<String, Thing> things;

    public Liveness() {
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
            thing.range.end = line;
        }

        String out = vAssign.dest.toString();
        Thing thing = things.get(out);
        if (thing == null) {
            thing = new Thing(out, line);
            things.put(out, thing);
        } else {
            thing.range.end = line;
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

            thing.range.end = line;
        }

        String in = vCall.addr.toString();
        Thing thing = things.get(in);
        if (out.equals(in))
            coalesce = true;
        thing.range.end = line;

        if (!coalesce) {
            thing = things.get(out);
            if (thing == null) {
                thing = new Thing(out, line);
                things.put(out, thing);
            } else {
                thing.range.end = line;
            }
        }
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

            thing.range.end = line;
        }

        if (!coalesce) {
            Thing thing = things.get(out);
            if (thing == null) {
                thing = new Thing(out, line);
                things.put(out, thing);
            } else {
                thing.range.end = line;
            }
        }
    }

    @Override
    public void visit(VMemWrite vMemWrite) throws Throwable {
        int line = vMemWrite.sourcePos.line;

        if (isVar(vMemWrite.source)) {
            String in = vMemWrite.source.toString();
            Thing thing = things.get(in);
            thing.range.end = line;
        }

        String out;
        if (isVar(vMemWrite.dest))
            out = ((VMemRef.Global) vMemWrite.dest).base.toString();
        else
            throw new Throwable();

        Thing thing = things.get(out);
        thing.range.end = line;
    }

    @Override
    public void visit(VMemRead vMemRead) throws Throwable {
        int line = vMemRead.sourcePos.line;

        if (isVar(vMemRead.source)) {
            String in = ((VMemRef.Global) vMemRead.source).base.toString();
            Thing thing = things.get(in);
            thing.range.end = line;
        }

        if (isVar(vMemRead.dest)) {
            String out = vMemRead.dest.toString();
            Thing thing = things.get(out);
            if (thing == null) {
                thing = new Thing(out, line);
                things.put(out, thing);
            } else {
                thing.range.end = line;
            }
        }
    }

    @Override
    public void visit(VBranch vBranch) throws Throwable {
        int line = vBranch.sourcePos.line;

        String in = vBranch.value.toString();
        Thing thing = things.get(in);
        thing.range.end = line;
    }

    @Override
    public void visit(VGoto vGoto) throws Throwable {
    }

    @Override
    public void visit(VReturn vReturn) throws Throwable {
        int line = vReturn.sourcePos.line;

        if (isVar(vReturn.value)) {
            String in = vReturn.value.toString();
            Thing thing = things.get(in);
            thing.range.end = line;
        }
    }
}

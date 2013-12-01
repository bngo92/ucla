import cs132.vapor.ast.*;

import java.util.LinkedHashMap;

public class CrossCall extends VInstr.Visitor<Throwable> {

    final LinkedHashMap<String, Liveness.Thing> registerMap;

    public CrossCall (LinkedHashMap<String, Liveness.Thing> registerMap) {
        this.registerMap = registerMap;
    }

    @Override
    public void visit(VAssign vAssign) throws Throwable {
    }

    @Override
    public void visit(VCall vCall) throws Throwable {
        for (Liveness.Thing thing : registerMap.values())
            if (thing.range.start < vCall.sourcePos.line && vCall.sourcePos.line < thing.range.end)
                thing.crossCall = true;
    }

    @Override
    public void visit(VBuiltIn vBuiltIn) throws Throwable {
    }

    @Override
    public void visit(VMemWrite vMemWrite) throws Throwable {
    }

    @Override
    public void visit(VMemRead vMemRead) throws Throwable {
    }

    @Override
    public void visit(VBranch vBranch) throws Throwable {
    }

    @Override
    public void visit(VGoto vGoto) throws Throwable {
    }

    @Override
    public void visit(VReturn vReturn) throws Throwable {
    }
}

import cs132.vapor.ast.*;

import java.util.LinkedHashMap;

class CrossCall extends VInstr.Visitor<Throwable> {

    private final LinkedHashMap<String, LivenessAnalysis.VarRef> registerMap;

    public CrossCall (LinkedHashMap<String, LivenessAnalysis.VarRef> registerMap) {
        this.registerMap = registerMap;
    }

    @Override
    public void visit(VAssign vAssign) throws Throwable {
    }

    @Override
    public void visit(VCall vCall) throws Throwable {
        for (LivenessAnalysis.VarRef varRef : registerMap.values())
            if (varRef.range.start < vCall.sourcePos.line && vCall.sourcePos.line < varRef.range.end)
                varRef.crossCall = true;
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

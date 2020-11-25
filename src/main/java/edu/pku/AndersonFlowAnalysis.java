package edu.pku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class AndersonFlowAnalysis extends ForwardFlowAnalysis<Unit, Anderson> {

    private static final Logger LOG = LoggerFactory.getLogger(AndersonFlowAnalysis.class);

    public AndersonFlowAnalysis(DirectedGraph<Unit> graph) {
        super(graph);
    }

    public void run() {
        doAnalysis();
    }

    @Override
    protected Anderson newInitialFlow() {
        return new Anderson();
    }

    @Override
    protected void copy(Anderson src, Anderson dest) {
        dest.clear();
        dest.addAllFrom(src);
    }

    @Override
    protected void merge(Anderson in1, Anderson in2, Anderson out) {
        out.clear();
        out.addAllFrom(in1);
        out.addAllFrom(in2);
    }

    @Override
    protected void flowThrough(Anderson in, Unit u, Anderson out) {
        out.clear();
        out.addAllFrom(in);

        if (u instanceof InvokeStmt) {
            InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
            if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
                Value v = ie.getArgs().get(1);
                int id = ((IntConstant) ie.getArgs().get(0)).value;
                QueryManager.addResults(id, out.getPointsToSet(v));
            } else if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
                out.setCurrAllocId(((IntConstant) ie.getArgs().get(0)).value);
                MemoryManager.addExplicitAllocId(out.getCurrAllocId());
            }
        } else if (u instanceof DefinitionStmt) {
            DefinitionStmt ds = (DefinitionStmt) u;
            if (ds.getRightOp() instanceof NewExpr) {
                if (out.getCurrAllocId() != 0)
                    out.addNewConstraint(out.getCurrAllocId(), ((DefinitionStmt) u).getLeftOp());
                else
                    out.addNewConstraint(MemoryManager.getNextAllocId(), ((DefinitionStmt) u).getLeftOp());
                out.setCurrAllocId(0);
            } else if (ds.getLeftOp() instanceof Local) {
                if (ds.getRightOp() instanceof Local) {
                    out.addAssignConstraint(ds.getRightOp(), ds.getLeftOp());
                } else if (ds.getRightOp() instanceof InstanceFieldRef) {
                    InstanceFieldRef ifr = (InstanceFieldRef) ds.getRightOp();
                    if (ifr.getBase() instanceof Local) {
                        out.addAssignFromHeapConstraint(ifr.getBase(), ds.getLeftOp(), ifr.getField().getName());
                    } else {
                        LOG.error("Unknown InstanceFieldRef base type: {}", ifr.getBase().getClass());
                    }
                } else if (ds.getRightOp() instanceof ThisRef) {
                    ThisRef rhs = (ThisRef)(ds.getRightOp());
                    out.addAssignFromHeapConstraint(rhs, ds.getLeftOp(), "");
                } else if (ds.getRightOp() instanceof StaticFieldRef) {
                    StaticFieldRef rhs = (StaticFieldRef) (ds.getRightOp());
                    String className = rhs.getField().getDeclaringClass().getName();
                    String fieldName = rhs.getField().getName();
                    out.addNewConstraint(MemoryManager.putStaticAllocId(className, fieldName), rhs);
                    out.addAssignFromHeapConstraint(rhs, ds.getLeftOp(), rhs.getField().getName());
                } else {
                    LOG.error("Unknown DefinitionStmt.getRightOP() base type: {}", ds.getRightOp().getClass());
                }
            } else if (ds.getLeftOp() instanceof InstanceFieldRef) {
                InstanceFieldRef lhs = (InstanceFieldRef) ds.getLeftOp();
                if (lhs.getBase() instanceof Local) {
                    if (ds.getRightOp() instanceof Local) {
                        out.addAssignToHeapConstraint(ds.getRightOp(), lhs.getBase(), lhs.getField().getName());
                    } else if (ds.getRightOp() instanceof Constant) {
                        // Nothing need to be done here
                    } else {
                        LOG.error("Unknown InstanceFieldRef base type: {}", ds.getRightOp().getClass());
                    }
                } else {
                    LOG.error("Unknown InstanceFieldRef base type: {}", lhs.getBase().getClass());
                }
            } else if (ds.getLeftOp() instanceof StaticFieldRef) {
                StaticFieldRef lhs = (StaticFieldRef) ds.getLeftOp();
                String className = lhs.getField().getDeclaringClass().getName();
                String fieldName = lhs.getField().getName();
                out.addNewConstraint(MemoryManager.putStaticAllocId(className, fieldName), lhs);
                if (ds.getRightOp() instanceof Local) {
                    out.addAssignToHeapConstraint(ds.getRightOp(), lhs, lhs.getField().getName());
                } else if (ds.getRightOp() instanceof Constant) {
                    // Nothing need to be done here
                } else {
                    LOG.error("Unknown StaticFieldRef base type: {}", ds.getRightOp().getClass());
                }
            } else {
                LOG.error("Unknown DefinitionStmt.getLeftOP() base type: {}", ds.getLeftOp().getClass());
            }
            out.run();
        }
    }
}

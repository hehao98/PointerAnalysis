package edu.pku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.ArrayList;
import java.util.List;

public class AndersonFlowAnalysis extends ForwardFlowAnalysis<Unit, Anderson> {

    private static final Logger LOG = LoggerFactory.getLogger(AndersonFlowAnalysis.class);

    private Anderson initialAnderson;
    private final Anderson finalAnderson = new Anderson();
    private final List<SootMethod> callStack = new ArrayList<>();

    public AndersonFlowAnalysis(DirectedGraph<Unit> graph) {
        super(graph);
    }

    public AndersonFlowAnalysis(DirectedGraph<Unit> graph, Anderson initialAnderson,
                                List<SootMethod> prevCallStack, SootMethod thisMethod) {
        super(graph);
        this.initialAnderson = initialAnderson;
        this.callStack.addAll(prevCallStack);
        this.callStack.add(thisMethod);
    }

    public void run() {
        doAnalysis();
    }

    @Override
    protected Anderson newInitialFlow() {
        Anderson anderson = new Anderson();
        if (initialAnderson != null) anderson.addAllFrom(initialAnderson);
        return anderson;
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
        LOG.info("    {}", u);

        out.clear();
        out.addAllFrom(in);

        if (u instanceof InvokeStmt) {
            InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
            if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
                Value v = ie.getArgs().get(1);
                int id = ((IntConstant) ie.getArgs().get(0)).value;
                if (v instanceof Local) {
                    LOG.info("    Query and Result {}: {}={}", id, v, out.getPointsToSet(v.toString()));
                    QueryManager.addResults(id, out.getPointsToSet(v.toString()));
                }
            } else if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
                out.setCurrAllocId(((IntConstant) ie.getArgs().get(0)).value);
                MemoryManager.addExplicitAllocId(out.getCurrAllocId());
            } else {
                handleMethodInvocation(ie.getMethod(), out, ie, null);
            }
        } else if (u instanceof DefinitionStmt) {
            DefinitionStmt ds = (DefinitionStmt) u;
            if (ds.getRightOp() instanceof InvokeExpr) {
                InvokeExpr ie = (InvokeExpr) ((DefinitionStmt) u).getRightOp();
                handleMethodInvocation(ie.getMethod(), out, ie, (Local) ds.getLeftOp());
            } else if (ds.getRightOp() instanceof NewExpr) {
                if (out.getCurrAllocId() != 0)
                    out.addNewConstraint(out.getCurrAllocId(), ((DefinitionStmt) u).getLeftOp().toString());
                else
                    out.addNewConstraint(MemoryManager.getNextAllocId(), ((DefinitionStmt) u).getLeftOp().toString());
                out.setCurrAllocId(0);
            } else if (ds.getRightOp() instanceof ParameterRef) {
                ParameterRef pr = (ParameterRef) (ds.getRightOp());
                if (ds.getLeftOp() instanceof Local) {
                    out.addAssignConstraint("arg" + pr.getIndex(), ds.getLeftOp().toString());
                } else {
                    LOG.error("Unknown ds.getLeftOP() type: {}", ds.getLeftOp().getClass());
                }
            } else if (ds.getLeftOp() instanceof Local) {
                if (ds.getRightOp() instanceof Local) {
                    out.addAssignConstraint(ds.getRightOp().toString(), ds.getLeftOp().toString());
                } else if (ds.getRightOp() instanceof InstanceFieldRef) {
                    InstanceFieldRef ifr = (InstanceFieldRef) ds.getRightOp();
                    if (ifr.getBase() instanceof Local) {
                        out.addAssignFromHeapConstraint(ifr.getBase().toString(), ds.getLeftOp().toString(), ifr.getField().getName());
                    } else {
                        LOG.error("Unknown InstanceFieldRef base type: {}", ifr.getBase().getClass());
                    }
                } else if (ds.getRightOp() instanceof ThisRef) {
                    ThisRef rhs = (ThisRef)(ds.getRightOp());
                    out.addAssignConstraint("this", rhs.toString());
                    out.addAssignConstraint(rhs.toString(), ds.getLeftOp().toString());
                    //out.addAssignFromHeapConstraint(rhs.toString(), ds.getLeftOp().toString(), "");
                } else if (ds.getRightOp() instanceof StaticFieldRef) {
                    StaticFieldRef rhs = (StaticFieldRef) (ds.getRightOp());
                    String className = rhs.getField().getDeclaringClass().getName();
                    String fieldName = rhs.getField().getName();
                    out.addNewConstraint(MemoryManager.putStaticAllocId(className, fieldName), rhs.toString());
                    out.addAssignFromHeapConstraint(rhs.toString(), ds.getLeftOp().toString(), rhs.getField().getName());
                } else {
                    LOG.error("Unknown DefinitionStmt.getRightOP() base type: {}", ds.getRightOp().getClass());
                }
            } else if (ds.getLeftOp() instanceof InstanceFieldRef) {
                InstanceFieldRef lhs = (InstanceFieldRef) ds.getLeftOp();
                if (lhs.getBase() instanceof Local) {
                    if (ds.getRightOp() instanceof Local) {
                        out.addAssignToHeapConstraint(ds.getRightOp().toString(), lhs.getBase().toString(), lhs.getField().getName());
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
                out.addNewConstraint(MemoryManager.putStaticAllocId(className, fieldName), lhs.toString());
                if (ds.getRightOp() instanceof Local) {
                    out.addAssignToHeapConstraint(ds.getRightOp().toString(), lhs.toString(), lhs.getField().getName());
                } else if (ds.getRightOp() instanceof Constant) {
                    // Nothing need to be done here
                } else {
                    LOG.error("Unknown StaticFieldRef base type: {}", ds.getRightOp().getClass());
                }
            } else {
                LOG.error("Unknown DefinitionStmt.getLeftOP() base type: {}", ds.getLeftOp().getClass());
            }
            out.run();
        } else if (u instanceof ReturnStmt || u instanceof ReturnVoidStmt) {
            if (u instanceof ReturnStmt) {
                ReturnStmt rs = (ReturnStmt) u;
                out.addAssignConstraint(rs.getOp().toString(), "ret");
                out.run();
            }
            finalAnderson.addAllFrom(out);
        }
    }

    private void handleMethodInvocation(SootMethod method, Anderson out, InvokeExpr ie, Local ret) {
        if (method.isJavaLibraryMethod()) {
            return;
        }
        // If the method is already called in call stack, skip it
        for (SootMethod prev : callStack) {
            if (prev.toString().equals(method.toString()))
                return;
        }

        DirectedGraph<Unit> thisGraph = new ExceptionalUnitGraph(method.retrieveActiveBody());
        Anderson initialState = new Anderson();
        for (int i = 0; i < ie.getArgs().size(); ++i) {
            Value v = ie.getArgs().get(i);
            initialState.addAllTo("arg" + i, out.getPointsToSet(v.toString()));
        }
        if (ie instanceof SpecialInvokeExpr) {
            Value v = ((SpecialInvokeExpr)ie).getBase();
            initialState.addAllTo("this", out.getPointsToSet(v.toString()));
        }
        initialState.addHeapFrom(out);

        LOG.info("Start Dataflow Analysis for Method {}", method);
        AndersonFlowAnalysis andersonFlowAnalysis = new AndersonFlowAnalysis(thisGraph, initialState, callStack, method);
        andersonFlowAnalysis.run();

        out.addHeapFrom(andersonFlowAnalysis.finalAnderson);
        if (ret != null && andersonFlowAnalysis.finalAnderson.getPointsToSet("ret") != null) {
            out.addAllTo(ret.toString(), andersonFlowAnalysis.finalAnderson.getPointsToSet("ret"));
        }
    }

}

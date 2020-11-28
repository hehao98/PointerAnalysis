package edu.pku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class AndersonFlowAnalysis extends ForwardFlowAnalysis<Unit, Anderson> {

    private static final Logger LOG = LoggerFactory.getLogger(AndersonFlowAnalysis.class);

    private Anderson initialAnderson = null;
    private final Anderson finalAnderson = new Anderson();
    private final List<SootMethod> callStack = new ArrayList<>();

    public AndersonFlowAnalysis(DirectedGraph<Unit> graph, SootMethod thisMethod) {
        super(graph);
        this.callStack.add(thisMethod);
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
        if (initialAnderson != null) {
            anderson.mergeAll(initialAnderson);
            anderson.setMemory(initialAnderson.getMemory().getCopy());
        }
        return anderson;
    }

    @Override
    protected void copy(Anderson src, Anderson dest) {
        dest.clear();
        dest.setCurrAllocId(src.getCurrAllocId());
        dest.setMemory(src.getMemory().getCopy());
        dest.mergeAll(src);
    }

    @Override
    protected void merge(Anderson in1, Anderson in2, Anderson out) {
        out.clear();
        if (in1.getCurrAllocId() != 0) out.setCurrAllocId(in1.getCurrAllocId());
        if (in2.getCurrAllocId() != 0) out.setCurrAllocId(in2.getCurrAllocId());
        out.mergeAll(in1);
        out.mergeAll(in2);
        out.setMemory(MemoryManager.merge(in1.getMemory(), in2.getMemory()));
    }

    @Override
    protected void flowThrough(Anderson in, Unit u, Anderson out) {
        if (PointerAnalyzer.exceedsTimeLimit()) {
            throw new RuntimeException("Time limit exceeded");
        }

        LOG.info("{},     {},    {}", u, graph.getPredsOf(u), graph.getSuccsOf(u));

        out.clear();
        out.setMemory(in.getMemory().getCopy());
        out.mergeAll(in);
        out.setCurrAllocId(in.getCurrAllocId());

        if (u instanceof InvokeStmt) {
            InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
            int currAllocId;
            if ((currAllocId = out.getMemory().extractTest(ie)) != 0) {
                Value v = ie.getArgs().get(1);
                LOG.info("    Query and Result {}: {}={}", currAllocId, v, out.getPointToSet(v.toString()));
                MemoryManager.addResultsToAllocId(currAllocId, out.getPointToSet(v.toString()));
            } else if ((currAllocId = out.getMemory().extractAlloc(ie)) != 0) {
                out.setCurrAllocId(currAllocId);
            } else {
                handleMethodInvocation(ie.getMethod(), out, ie, null);
            }
        } else if (u instanceof DefinitionStmt) {
            DefinitionStmt ds = (DefinitionStmt) u;
            if (ds.getRightOp() instanceof InvokeExpr) {
                InvokeExpr ie = (InvokeExpr) ((DefinitionStmt) u).getRightOp();
                handleMethodInvocation(ie.getMethod(), out, ie, (Local) ds.getLeftOp());
            } else if (ds.getRightOp() instanceof NewExpr) {
                String key = ds.getLeftOp().toString();
                SootClass allocClass = ((NewExpr) ds.getRightOp()).getBaseType().getSootClass();
                if (out.getCurrAllocId() > 0) {
                    out.getMemory().initAllocId(out.getCurrAllocId(), allocClass);
                    out.add(key, out.getCurrAllocId());
                } else {
                    int id = out.getMemory().initImplicitAllocId(callStack.get(callStack.size() - 1), u, allocClass);
                    out.add(key, id);
                }
                out.setCurrAllocId(0);
            } else if (ds.getRightOp() instanceof ParameterRef) {
                ParameterRef pr = (ParameterRef) (ds.getRightOp());
                if (ds.getLeftOp() instanceof Local) {
                    out.replace("arg" + pr.getIndex(), ds.getLeftOp().toString());
                } else {
                    LOG.error("Unknown ds.getLeftOP() type: {}", ds.getLeftOp().getClass());
                }
            } else if (ds.getLeftOp() instanceof Local) {
                Local lhs = (Local)(ds.getLeftOp());
                if (ds.getRightOp() instanceof Local) {
                    out.replace(ds.getRightOp().toString(), lhs.toString());
                } else if (ds.getRightOp() instanceof InstanceFieldRef) {
                    InstanceFieldRef ifr = (InstanceFieldRef) ds.getRightOp();
                    if (ifr.getBase() instanceof Local) {
                        String fieldName = ifr.getField().getName();
                        TreeSet<Integer> allocIds = out.getPointToSet(ifr.getBase().toString());
                        out.replaceWith(lhs.toString(), out.getMemory().getPointToSet(allocIds, fieldName));
                    } else {
                        LOG.error("Unknown InstanceFieldRef base type: {}", ifr.getBase().getClass());
                    }
                } else if (ds.getRightOp() instanceof ThisRef) {
                    out.replace("this", ds.getLeftOp().toString());
                } else if (ds.getRightOp() instanceof StaticFieldRef) {
                    StaticFieldRef rhs = (StaticFieldRef) (ds.getRightOp());
                    int id = out.getMemory().initStaticAllocId(
                            rhs.getField().getDeclaringClass().getName(),
                            rhs.getField().getName(),
                            rhs.getField().getDeclaringClass()
                    );
                    out.replaceWith(lhs.toString(), out.getMemory().getPointToSet(id, rhs.getField().getName()));
                } else {
                    LOG.error("Unknown DefinitionStmt.getRightOP() base type: {}", ds.getRightOp().getClass());
                }
            } else if (ds.getLeftOp() instanceof InstanceFieldRef) {
                InstanceFieldRef lhs = (InstanceFieldRef) ds.getLeftOp();
                if (lhs.getBase() instanceof Local) {
                    if (ds.getRightOp() instanceof Local) { // a.f = b
                        LOG.info("{} ", out.getMemory().getMemAllocTable());
                        LOG.info( "{} {} {}",
                                out.getPointToSet(lhs.getBase().toString()),
                                lhs.getField().getName(),
                                out.getPointToSet(ds.getRightOp().toString())
                        );
                        out.getMemory().updatePointToSet(
                                out.getPointToSet(lhs.getBase().toString()),
                                lhs.getField().getName(),
                                out.getPointToSet(ds.getRightOp().toString())
                        );
                    } else if (!(ds.getRightOp() instanceof Constant)) {
                        LOG.error("Unknown InstanceFieldRef base type: {}", ds.getRightOp().getClass());
                    }
                } else {
                    LOG.error("Unknown InstanceFieldRef base type: {}", lhs.getBase().getClass());
                }
            } else if (ds.getLeftOp() instanceof StaticFieldRef) { // A.f = b
                StaticFieldRef lhs = (StaticFieldRef) ds.getLeftOp();
                int id = out.getMemory().initStaticAllocId(
                        lhs.getField().getDeclaringClass().getName(),
                        lhs.getField().getName(),
                        lhs.getField().getDeclaringClass()
                );
                LOG.info("{}", id);
                if (ds.getRightOp() instanceof Local) {
                    out.getMemory().updatePointToSet(
                            id,
                            lhs.getField().getName(),
                            out.getPointToSet(ds.getRightOp().toString())
                    );
                } else if (!(ds.getRightOp() instanceof Constant)) {
                    LOG.error("Unknown StaticFieldRef base type: {}", ds.getRightOp().getClass());
                }
            } else {
                LOG.error("Unknown DefinitionStmt.getLeftOP() base type: {}", ds.getLeftOp().getClass());
            }
        } else if (u instanceof ReturnStmt || u instanceof ReturnVoidStmt) {
            if (u instanceof ReturnStmt) {
                ReturnStmt rs = (ReturnStmt) u;
                out.merge(rs.getOp().toString(), "ret");
            }
            finalAnderson.mergeAll(out);
            finalAnderson.setMemory(MemoryManager.merge(finalAnderson.getMemory(), out.getMemory()));
        }

        LOG.info("    State Before: {}", in.getPointToSet());
        LOG.info("    State After : {}", out.getPointToSet());
        LOG.info("    Heap Table Before: {}", in.getMemory().getMemAllocTable());
        LOG.info("    Heap Table After : {}, {}", out.getMemory().getMemAllocTable(), out.equals(in));
        LOG.info("    Static Alloc Table: {}", MemoryManager.getStaticAllocIds());
        LOG.info("    Implicit Alloc Table: {}", MemoryManager.getImplicitAllocIds());
    }

    private void handleMethodInvocation(SootMethod method, Anderson out, InvokeExpr ie, Local ret) {
        if (method.isJavaLibraryMethod()) {
            return;
        }
        int duplicateMethodCount = 0;
        for (SootMethod prev : callStack) {
            if (prev.toString().equals(method.toString()))
                duplicateMethodCount++;
        }
        if (duplicateMethodCount > 1) return;

        DirectedGraph<Unit> thisGraph = new ExceptionalUnitGraph(method.retrieveActiveBody());
        Anderson initialState = new Anderson();
        initialState.setMemory(out.getMemory().getCopy());
        for (int i = 0; i < ie.getArgs().size(); ++i) {
            Value v = ie.getArgs().get(i);
            initialState.mergeAll("arg" + i, out.getPointToSet(v.toString()));
        }
        if (ie instanceof SpecialInvokeExpr) {
            Value v = ((SpecialInvokeExpr)ie).getBase();
            initialState.mergeAll("this", out.getPointToSet(v.toString()));
        }

        LOG.info("Start Dataflow Analysis for Method {}", method);
        AndersonFlowAnalysis andersonFlowAnalysis = new AndersonFlowAnalysis(thisGraph, initialState, callStack, method);
        andersonFlowAnalysis.run();

        out.setMemory(andersonFlowAnalysis.finalAnderson.getMemory().getCopy());
        if (ret != null) {
            out.replaceWith(ret.toString(), andersonFlowAnalysis.finalAnderson.getPointToSet("ret"));
        }
    }

}

package edu.pku;

import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.invoke.SiteInliner;
import soot.util.queue.QueueReader;

public class WholeProgramTransformer extends SceneTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(WholeProgramTransformer.class);
    private final TreeMap<Integer, Local> queries = new TreeMap<>();
    private final Set<Integer> allocIds = new TreeSet<>();
    private final Anderson anderson = new Anderson();
    private int nextAllocId = -1;

    private void extractConstraints(List<SootMethod> methodsToAnalyze) {
        int allocId = 0;
        for (SootMethod sm : methodsToAnalyze) {
            LOG.info("Analyzing method {}", sm.toString());
            for (Unit u : sm.getActiveBody().getUnits()) {
                LOG.info("    {}", u);
                if (u instanceof InvokeStmt) {
                    InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
                    if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
                        Value v = ie.getArgs().get(1);
                        int id = ((IntConstant) ie.getArgs().get(0)).value;
                        queries.put(id, (Local) v);
                    } else if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
                        allocId = ((IntConstant) ie.getArgs().get(0)).value;
                        allocIds.add(allocId);
                    }
                } else if (u instanceof DefinitionStmt) {
                    DefinitionStmt ds = (DefinitionStmt) u;
                    if (ds.getRightOp() instanceof NewExpr) {
                        if (allocId != 0)
                            anderson.addNewConstraint(allocId, (Local) ((DefinitionStmt) u).getLeftOp());
                        else
                            anderson.addNewConstraint(nextAllocId--, (Local) ((DefinitionStmt) u).getLeftOp());
                        allocId = 0;
                    } else if (ds.getLeftOp() instanceof Local) {
                        if (ds.getRightOp() instanceof Local) {
                            anderson.addAssignConstraint((Local) ds.getRightOp(), (Local) ds.getLeftOp());
                        } else if (ds.getRightOp() instanceof InstanceFieldRef) {
                            InstanceFieldRef ifr = (InstanceFieldRef) ds.getRightOp();
                            if (ifr.getBase() instanceof Local) {
                                anderson.addAssignFromHeapConstraint((Local) (ifr.getBase()), (Local) ds.getLeftOp(), ifr.getField().getName());
                            } else {
                                LOG.error("Unknown InstanceFieldRef base type: {}", ifr.getBase().getClass());
                            }
                        } else {
                            LOG.error("Unknown DefinitionStmt.getRightOP() base type: {}", ds.getRightOp().getClass());
                        }
                    } else if (ds.getLeftOp() instanceof InstanceFieldRef) {
                        InstanceFieldRef lhs = (InstanceFieldRef) ds.getLeftOp();
                        if (lhs.getBase() instanceof Local) {
                            if (ds.getRightOp() instanceof Local) {
                                anderson.addAssignToHeapConstraint((Local) ds.getRightOp(), (Local) (lhs.getBase()), lhs.getField().getName());
                            } else {
                                LOG.error("Unknown InstanceFieldRef base type: {}", lhs.getBase().getClass());
                            }
                        } else {
                            LOG.error("Unknown InstanceFieldRef base type: {}", lhs.getBase().getClass());
                        }
                    } else {
                        LOG.error("Unknown DefinitionStmt.getLeftOP() base type: {}", ds.getLeftOp().getClass());
                    }
                }
            }
        }
    }

    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {

        // Find the entry point with which the real program begins (exclude JDK built-in entry points)
        List<SootMethod> entryPoints = new ArrayList<>();
        for (SootMethod sm : Scene.v().getEntryPoints()) {
            if (!sm.getDeclaringClass().isJavaLibraryClass()) {
                entryPoints.add(sm);
            }
        }
        LOG.info("Entry points for this program: {}", entryPoints);

        //ReachableMethods reachableMethods = new ReachableMethods(Scene.v().getCallGraph(), entryPoints);
        ReachableMethods reachableMethods = Scene.v().getReachableMethods();
        QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();

        // We implement first-order context sensitivity through method inlining
        List<SootMethod> methodsToAnalyze = new ArrayList<>();
        while (qr.hasNext()) {
            SootMethod sm = qr.next().method();
            if (sm.getDeclaringClass().isJavaLibraryClass()) // Skip internal methods
                continue;
            if (sm.hasActiveBody()) {
                Map<Stmt, SootMethod> methodsToInline = new HashMap<>();
                for (Unit u : sm.getActiveBody().getUnits()) {
                    if (u instanceof InvokeStmt) {
                        InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
                        if (!ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")
                                && !ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
                            methodsToInline.put((InvokeStmt) u, ie.getMethod());
                        }
                    }
                    if (u instanceof DefinitionStmt) {
                        if (((DefinitionStmt) u).getRightOp() instanceof InvokeExpr) {
                            InvokeExpr ie = (InvokeExpr) ((DefinitionStmt) u).getRightOp();
                            methodsToInline.put((DefinitionStmt) u, ie.getMethod());
                        }
                    }
                }
                // Perform inlining before the method is passed for analysis
                for (Entry<Stmt, SootMethod> entry : methodsToInline.entrySet()) {
                    SiteInliner.inlineSite(entry.getValue(), entry.getKey(), sm);
                }
                methodsToAnalyze.add(sm);
            }
        }

        extractConstraints(methodsToAnalyze);

        anderson.run();

        LOG.info("Queries: {}", queries);
        StringBuilder answer = new StringBuilder();
        for (Entry<Integer, Local> q : queries.entrySet()) {
            TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
            LOG.info("Query ({})={}", q, result);
            answer.append(q.getKey().toString()).append(":");
            if (result != null && result.size() > 0) {
                for (Integer i : result) {
                    if (i <= 0) continue;
                    answer.append(" ").append(i);
                }
            } else {
                for (Integer i : allocIds) {
                    answer.append(" ").append(i);
                }
            }
            answer.append("\n");
        }
        AnswerPrinter.printAnswer(answer.toString());

    }

}

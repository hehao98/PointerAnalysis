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
    private final TreeMap<Integer, List<Local>> queries = new TreeMap<>();
    private final Set<Integer> allocIds = new TreeSet<>();
    private final Map<String, Integer> staticAllocIds = new HashMap<>();
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
                        queries.computeIfAbsent(id, k -> new ArrayList<>()).add((Local) v);
                    } else if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
                        allocId = ((IntConstant) ie.getArgs().get(0)).value;
                        allocIds.add(allocId);
                    }
                } else if (u instanceof DefinitionStmt) {
                    DefinitionStmt ds = (DefinitionStmt) u;
                    if (ds.getRightOp() instanceof NewExpr) {
                        if (allocId != 0)
                            anderson.addNewConstraint(allocId, ((DefinitionStmt) u).getLeftOp());
                        else
                            anderson.addNewConstraint(nextAllocId--, ((DefinitionStmt) u).getLeftOp());
                        allocId = 0;
                    } else if (ds.getLeftOp() instanceof Local) {
                        if (ds.getRightOp() instanceof Local) {
                            anderson.addAssignConstraint(ds.getRightOp(), ds.getLeftOp());
                        } else if (ds.getRightOp() instanceof InstanceFieldRef) {
                            InstanceFieldRef ifr = (InstanceFieldRef) ds.getRightOp();
                            if (ifr.getBase() instanceof Local) {
                                anderson.addAssignFromHeapConstraint(ifr.getBase(), ds.getLeftOp(), ifr.getField().getName());
                            } else {
                                LOG.error("Unknown InstanceFieldRef base type: {}", ifr.getBase().getClass());
                            }
                        } else if (ds.getRightOp() instanceof ThisRef) {
                            ThisRef rhs = (ThisRef)(ds.getRightOp());
                            anderson.addAssignFromHeapConstraint(rhs, ds.getLeftOp(), "");
                        } else if (ds.getRightOp() instanceof StaticFieldRef) {
                            StaticFieldRef rhs = (StaticFieldRef) (ds.getRightOp());
                            String className = rhs.getField().getDeclaringClass().getName();
                            if (!staticAllocIds.containsKey(className)) staticAllocIds.put(className, nextAllocId--);
                            anderson.addNewConstraint(staticAllocIds.get(className), rhs);
                            anderson.addAssignFromHeapConstraint(rhs, ds.getLeftOp(), rhs.getField().getName());
                        } else {
                            LOG.error("Unknown DefinitionStmt.getRightOP() base type: {}", ds.getRightOp().getClass());
                        }
                    } else if (ds.getLeftOp() instanceof InstanceFieldRef) {
                        InstanceFieldRef lhs = (InstanceFieldRef) ds.getLeftOp();
                        if (lhs.getBase() instanceof Local) {
                            if (ds.getRightOp() instanceof Local) {
                                anderson.addAssignToHeapConstraint(ds.getRightOp(), lhs.getBase(), lhs.getField().getName());
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
                        if (!staticAllocIds.containsKey(className)) staticAllocIds.put(className, nextAllocId--);
                        anderson.addNewConstraint(staticAllocIds.get(className), lhs);
                        if (ds.getRightOp() instanceof Local) {
                            anderson.addAssignToHeapConstraint(ds.getRightOp(), lhs, lhs.getField().getName());
                        } else if (ds.getRightOp() instanceof Constant) {
                            // Nothing need to be done here
                        } else {
                            LOG.error("Unknown StaticFieldRef base type: {}", ds.getRightOp().getClass());
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
        for (Entry<Integer, List<Local>> q : queries.entrySet()) {
            LOG.info("Query: id={}, locals={}", q.getKey(), q.getValue());
            TreeSet<Integer> result = new TreeSet<>();
            for (Local local : q.getValue()) {
                LOG.info("    local {}={}", local, anderson.getPointsToSet(local));
                if (anderson.getPointsToSet(local) != null) {
                    result.addAll(anderson.getPointsToSet(local));
                }
            }
            answer.append(q.getKey().toString()).append(":");
            if (result.size() > 0) {
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

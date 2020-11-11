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
import soot.util.queue.QueueReader;

public class WholeProgramTransformer extends SceneTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(WholeProgramTransformer.class);

    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {

        TreeMap<Integer, Local> queries = new TreeMap<>();
        Anderson anderson = new Anderson();

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
        Set<Integer> allocIds = new TreeSet<>();
        while (qr.hasNext()) {
            SootMethod sm = qr.next().method();
            if (sm.getDeclaringClass().isJavaLibraryClass()) // Skip internal methods
                continue;
            LOG.info("Analyzing method {}", sm.toString());
            int allocId = 0;
            if (sm.hasActiveBody()) {
                for (Unit u : sm.getActiveBody().getUnits()) {
                    LOG.info("    {}", u);
                    if (u instanceof InvokeStmt) {
                        InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
                        if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
                            allocId = ((IntConstant) ie.getArgs().get(0)).value;
                            allocIds.add(allocId);
                        }
                        if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
                            Value v = ie.getArgs().get(1);
                            int id = ((IntConstant) ie.getArgs().get(0)).value;
                            queries.put(id, (Local) v);
                        }
                    }
                    if (u instanceof DefinitionStmt) {
                        DefinitionStmt ds = (DefinitionStmt) u;
                        if (ds.getRightOp() instanceof NewExpr) {
                            anderson.addNewConstraint(allocId, (Local) ((DefinitionStmt) u).getLeftOp());
                        } else if (ds.getLeftOp() instanceof Local) {
                            if (ds.getRightOp() instanceof Local) {
                                anderson.addAssignConstraint((Local) ds.getRightOp(), (Local) ds.getLeftOp());
                            } else if (ds.getRightOp() instanceof InstanceFieldRef) {
                                InstanceFieldRef ifr = (InstanceFieldRef) ds.getRightOp();
                                if (ifr.getBase() instanceof Local) {
                                    anderson.addAssignFromHeapConstraint((Local)(ifr.getBase()), (Local) ds.getLeftOp(), ifr.getField().getName());
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
                                    anderson.addAssignToHeapConstraint((Local)ds.getRightOp(), (Local)(lhs.getBase()), lhs.getField().getName());
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

        anderson.run();

        LOG.info("Queries: {}", queries);
        StringBuilder answer = new StringBuilder();
        for (Entry<Integer, Local> q : queries.entrySet()) {
            TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
            answer.append(q.getKey().toString()).append(":");
            if (result != null && result.size() > 0) {
                for (Integer i : result) {
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

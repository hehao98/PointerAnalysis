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
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
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
        while (qr.hasNext()) {
            SootMethod sm = qr.next().method();
            if (sm.getDeclaringClass().isJavaLibraryClass()) // Skip internal methods
                continue;
            LOG.info("Analyzing method {}", sm.toString());
            int allocId = 0;
            if (sm.hasActiveBody()) {
                for (Unit u : sm.getActiveBody().getUnits()) {
                    LOG.info("{}: {}", u.getClass(), u);
                    //System.out.println("S: " + u);
                    //System.out.println(u.getClass());
                    if (u instanceof InvokeStmt) {
                        InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
                        if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
                            allocId = ((IntConstant) ie.getArgs().get(0)).value;
                        }
                        if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
                            Value v = ie.getArgs().get(1);
                            int id = ((IntConstant) ie.getArgs().get(0)).value;
                            queries.put(id, (Local) v);
                        }
                    }
                    if (u instanceof DefinitionStmt) {
                        if (((DefinitionStmt) u).getRightOp() instanceof NewExpr) {
                            //System.out.println("Alloc " + allocId);
                            anderson.addNewConstraint(allocId, (Local) ((DefinitionStmt) u).getLeftOp());
                        }
                        if (((DefinitionStmt) u).getLeftOp() instanceof Local && ((DefinitionStmt) u).getRightOp() instanceof Local) {
                            anderson.addAssignConstraint((Local) ((DefinitionStmt) u).getRightOp(), (Local) ((DefinitionStmt) u).getLeftOp());
                        }
                    }
                }
            }
            //}
        }

        anderson.run();

        LOG.info("Queries: {}", queries);
        String answer = "";
        for (Entry<Integer, Local> q : queries.entrySet()) {
            TreeSet<Integer> result = anderson.getPointsToSet(q.getValue());
            answer += q.getKey().toString() + ":";
            if (result != null) {
                for (Integer i : result) {
                    answer += " " + i;
                }
            } else {
                for (Integer i : queries.keySet()) {
                    answer += " " + i;
                }
            }
            answer += "\n";
        }
        AnswerPrinter.printAnswer(answer);

    }

}

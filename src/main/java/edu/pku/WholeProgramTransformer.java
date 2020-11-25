package edu.pku;

import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.invoke.SiteInliner;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.queue.QueueReader;

public class WholeProgramTransformer extends SceneTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(WholeProgramTransformer.class);


    private void analyzePointer(List<SootMethod> methodsToAnalyze) {
        for (SootMethod sm : methodsToAnalyze) {
            LOG.info("Analyzing method {}", sm.toString());
            DirectedGraph<Unit> graph = new ExceptionalUnitGraph(sm.retrieveActiveBody());
            AndersonFlowAnalysis andersonFlowAnalysis = new AndersonFlowAnalysis(graph);
            andersonFlowAnalysis.run();
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

        SootMethod m = Scene.v().getMainClass().getMethodByName("main");

        // ReachableMethods reachableMethods = new ReachableMethods(Scene.v().getCallGraph(), entryPoints);
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

        analyzePointer(methodsToAnalyze);

        LOG.info("Queries: {}", QueryManager.queries);
        StringBuilder answer = new StringBuilder();
        for (Entry<Integer, TreeSet<Integer>> q : QueryManager.result.entrySet()) {
            answer.append(q.getKey().toString()).append(":");
            if (q.getValue().size() > 0) {
                // boolean hasImplicitAllocId = false;
                for (Integer i : q.getValue()) {
                    if (i <= 0) {
                        // hasImplicitAllocId = true;
                        // answer.append(" ").append(0);
                        continue;
                    }
                    answer.append(" ").append(i);
                }
            } else {
                for (Integer i : MemoryManager.getAllocIds()) {
                    answer.append(" ").append(i);
                }
            }
            answer.append("\n");
        }
        AnswerPrinter.printAnswer(answer.toString());
    }

}

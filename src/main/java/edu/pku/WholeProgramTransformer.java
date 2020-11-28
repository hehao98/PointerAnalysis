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

    private static final TreeSet<Integer> allocIds = new TreeSet<>();

    private void extractAllQueries() {
        ReachableMethods reachableMethods = Scene.v().getReachableMethods();
        QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();

        while (qr.hasNext()) {
            SootMethod sm = qr.next().method();
            if (sm.getDeclaringClass().isJavaLibraryClass()) // Skip internal methods
                continue;
            if (sm.hasActiveBody()) {
                for (Unit u : sm.getActiveBody().getUnits()) {
                    if (u instanceof InvokeStmt) {
                        InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
                        if (ie.getMethod().toString().equals(MemoryManager.TEST)) {
                            int id = ((IntConstant) ie.getArgs().get(0)).value;
                            MemoryManager.getResults().computeIfAbsent(id, k -> new TreeSet<>());
                        } else if (ie.getMethod().toString().equals(MemoryManager.ALLOC)) {
                            allocIds.add(((IntConstant) ie.getArgs().get(0)).value);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        extractAllQueries();
        LOG.info("Queries: {}", MemoryManager.getResults());

        SootMethod m = Scene.v().getMainClass().getMethodByName("main");

        /*
        for (int i = 0; i < 10; ++i) {
            Map<Stmt, SootMethod> methodsToInline = new HashMap<>();
            for (Unit u : m.getActiveBody().getUnits()) {
                if (u instanceof InvokeStmt) {
                    InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
                    if (ie.getMethod().isJavaLibraryMethod()) continue;
                    if (!ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")
                            && !ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>")) {
                        methodsToInline.put((InvokeStmt) u, ie.getMethod());
                    }
                }
                if (u instanceof DefinitionStmt) {
                    if (((DefinitionStmt) u).getRightOp() instanceof InvokeExpr) {
                        InvokeExpr ie = (InvokeExpr) ((DefinitionStmt) u).getRightOp();
                        if (ie.getMethod().isJavaLibraryMethod()) continue;
                        methodsToInline.put((DefinitionStmt) u, ie.getMethod());
                    }
                }
            }
            // Perform inlining before the method is passed for analysis
            for (Entry<Stmt, SootMethod> entry : methodsToInline.entrySet()) {
                SiteInliner.inlineSite(entry.getValue(), entry.getKey(), m);
            }
        }*/

        try {
            DirectedGraph<Unit> graph = new ExceptionalUnitGraph(m.retrieveActiveBody());
            AndersonFlowAnalysis andersonFlowAnalysis = new AndersonFlowAnalysis(graph, m);
            andersonFlowAnalysis.run();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            for (StackTraceElement s : e.getStackTrace())
                LOG.error("    {}", s);
            for (int key : MemoryManager.getResults().keySet()) {
                MemoryManager.getResults().get(key).clear();
            }
        }

        LOG.info("Queries: {}", MemoryManager.getResults());
        StringBuilder answer = new StringBuilder();
        for (Entry<Integer, TreeSet<Integer>> q : MemoryManager.getResults().entrySet()) {
            answer.append(q.getKey().toString()).append(":");
            if (q.getValue().size() > 0) {
                boolean hasImplicitAllocId = false;
                for (Integer i : q.getValue()) {
                    if (i <= 0 && !hasImplicitAllocId) {
                        hasImplicitAllocId = true;
                        answer.append(" ").append(0);
                        continue;
                    }
                    answer.append(" ").append(i);
                }
            } else {
                answer.append(" ").append(0);
                for (Integer i : allocIds) {
                    if (i > 0) answer.append(" ").append(i);
                }
            }
            answer.append("\n");
        }
        AnswerPrinter.printAnswer(answer.toString());
    }

}

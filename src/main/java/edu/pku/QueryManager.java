package edu.pku;

import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;

import java.util.TreeMap;
import java.util.TreeSet;

public class QueryManager {
    private static final String ALLOC = "<benchmark.internal.BenchmarkN: void alloc(int)>";
    private static final String TEST = "<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>";
    private static final TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<>();

    public static int extractTest(InvokeExpr ie) {
        if (ie.getMethod().toString().equals(TEST)) {
            int id = ((IntConstant) ie.getArgs().get(0)).value;
            addResultsToId(id, new TreeSet<>());
            return id;
        }
        return 0;
    }

    public static int extractAlloc(InvokeExpr ie) {
        if (ie.getMethod().toString().equals(ALLOC)) {
            int currAllocId = ((IntConstant) ie.getArgs().get(0)).value;
            MemoryManager.addExplicitAllocId(currAllocId);
            return currAllocId;
        }
        return 0;
    }

    public static void addResultsToId(int id, TreeSet<Integer> points) {
        if (points == null) return;
        result.computeIfAbsent(id, k -> new TreeSet<>());
        result.get(id).addAll(points);
    }

    public static TreeMap<Integer, TreeSet<Integer>> getQueryResults() {
        return result;
    }
}

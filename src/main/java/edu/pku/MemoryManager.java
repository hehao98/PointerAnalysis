package edu.pku;

import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;

import java.util.*;

public class MemoryManager {
    private static final String ALLOC = "<benchmark.internal.BenchmarkN: void alloc(int)>";
    private static final String TEST = "<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>";
    private static final TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<>();
    private static final Set<Integer> allocIds = new TreeSet<>();
    private static final Map<String, Integer> staticAllocIds = new HashMap<>();
    private static int nextAllocId = -1;

    public static int extractTest(InvokeExpr ie) {
        if (ie.getMethod().toString().equals(TEST)) {
            int id = ((IntConstant) ie.getArgs().get(0)).value;
            addResultsToAllocId(id, new TreeSet<>());
            return id;
        }
        return 0;
    }

    public static int extractAlloc(InvokeExpr ie) {
        if (ie.getMethod().toString().equals(ALLOC)) {
            int currAllocId = ((IntConstant) ie.getArgs().get(0)).value;
            addExplicitAllocId(currAllocId);
            return currAllocId;
        }
        return 0;
    }

    public static void addResultsToAllocId(int id, TreeSet<Integer> points) {
        if (points == null) return;
        result.computeIfAbsent(id, k -> new TreeSet<>());
        result.get(id).addAll(points);
    }

    public static TreeMap<Integer, TreeSet<Integer>> getResults() {
        return result;
    }

    public static void addExplicitAllocId(int id) {
        allocIds.add(id);
    }

    public static Set<Integer> getExplicitAllocIds() {
        return allocIds;
    }

    public static int getNextImplicitAllocId() {
        return nextAllocId--;
    }

    public static int putStaticAllocId(String className, String fieldName) {
        String key = className + "." + fieldName;
        if (!staticAllocIds.containsKey(key))
            staticAllocIds.put(key, nextAllocId--);
        return staticAllocIds.get(key);
    }
}

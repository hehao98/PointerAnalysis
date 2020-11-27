package edu.pku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootClass;
import soot.SootField;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;

import java.util.*;

public class MemoryManager {

    private static class Item {
        final String type;
        final TreeSet<Integer> points;
        Item(String type, TreeSet<Integer> points) {
            this.type = type;
            this.points = points;
        }
    }

    public static final String ALLOC = "<benchmark.internal.BenchmarkN: void alloc(int)>";
    public static final String TEST = "<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>";
    public static final String THIS = "";
    private static final Logger LOG = LoggerFactory.getLogger(MemoryManager.class);
    private static final TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<>();
    private static final Set<Integer> allocIds = new TreeSet<>();
    private static final Map<Object, Map<String, Item>> id2f2s = new HashMap<>(); // allocId -> field -> pointSet
    private static final Map<Object, String> id2type = new HashMap<>();           // allocId -> type
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
        if (id <= 0) LOG.error("Illegal alloc id");
        allocIds.add(id);
    }

    public static Set<Integer> getAllocIds() {
        return allocIds;
    }

    public static int getNextImplicitAllocId() {
        return nextAllocId--;
    }

    public static void initStaticAllocId(String className, String fieldName, SootClass fieldClass) {
        String id = className + "." + fieldName;
        initAllocId(id, fieldClass);
    }

    public static void initAllocId(Object id, SootClass c) {
        id2type.put(id, c.getName());
        id2f2s.put(id, new HashMap<>());
        id2f2s.get(id).put(THIS, new Item(c.getName(), new TreeSet<>()));
        for (SootField field : c.getFields()) {
            id2f2s.get(id).put(field.getName(), new Item(field.getType().toString(), new TreeSet<>()));
        }
    }

    public static TreeSet<Integer> getPointToSet(Object id, String field) {
        return id2f2s.get(id).get(field).points;
    }

    public static TreeSet<Integer> getPointToSet(TreeSet<Integer> ids, String field) {

    }
}

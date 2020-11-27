package edu.pku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
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
        @Override public String toString() {
            return points.toString();
        }
    }

    public static final String ALLOC = "<benchmark.internal.BenchmarkN: void alloc(int)>";
    public static final String TEST = "<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>";
    public static final String THIS = "";
    private static final Logger LOG = LoggerFactory.getLogger(MemoryManager.class);
    private static final TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<>();
    private static final Set<Integer> allocIds = new TreeSet<>();
    private static final Map<Integer, Map<String, Item>> id2f2s = new HashMap<>(); // allocId -> field -> pointSet
    private static final Map<Integer, String> id2type = new HashMap<>();           // allocId -> type
    private static final Map<String, Integer> static2Id = new HashMap<>();         // className.field -> allocId
    private static final Map<String, Integer> implicit2Id = new HashMap<>();       // className.method.unit -> allocId
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
        int id = nextAllocId--;
        allocIds.add(id);
        return id;
    }

    public static int initStaticAllocId(String className, String fieldName, SootClass fieldClass) {
        String key = className + "." + fieldName;
        if (static2Id.containsKey(key)) return static2Id.get(key);
        int id = getNextImplicitAllocId();
        static2Id.put(key, id);
        initAllocId(id, fieldClass);
        return id;
    }

    public static int initImplicitAllocId(SootMethod method, Unit u, SootClass allocClass) {
        String key = method.getDeclaringClass().getName() + "." + method.getName() + "." + u.toString();
        if (implicit2Id.containsKey(key)) return implicit2Id.get(key);
        int id = getNextImplicitAllocId();
        implicit2Id.put(key, id);
        initAllocId(id, allocClass);
        return id;
    }

    public static void initAllocId(int id, SootClass c) {
        id2type.put(id, c.getName());
        id2f2s.put(id, new HashMap<>());
        id2f2s.get(id).put(THIS, new Item(c.getName(), new TreeSet<>()));
        for (SootField field : c.getFields()) {
            id2f2s.get(id).put(field.getName(), new Item(field.getType().toString(), new TreeSet<>()));
        }
    }

    public static TreeSet<Integer> getPointToSet(int id, String field) {
        return id2f2s.get(id).get(field).points;
    }

    public static TreeSet<Integer> getPointToSet(Collection<Integer> ids, String field) {
        TreeSet<Integer> result = new TreeSet<>();
        for (Integer id : ids) {
            result.addAll(getPointToSet(id, field));
        }
        return result;
    }

    public static void replacePointToSet(int id, String field, Collection<Integer> points) {
        getPointToSet(id, field).clear();
        getPointToSet(id, field).addAll(points);
    }

    public static void replacePointToSet(Collection<Integer> ids, String field, Collection<Integer> points) {
        for (Integer id : ids) {
            getPointToSet(id, field).clear();
            getPointToSet(id, field).addAll(points);
        }
    }

    public static Map<Integer, Map<String, Item>> getMemAllocTable() {
        return id2f2s;
    }

    public static int getStaticAllocId(String className, String fieldName) {
        return static2Id.get(className + "." + fieldName);
    }

    public static Map<String, Integer> getStaticAllocIds() {
        return static2Id;
    }

    public static Map<String, Integer> getImplicitAllocIds() {
        return implicit2Id;
    }
}

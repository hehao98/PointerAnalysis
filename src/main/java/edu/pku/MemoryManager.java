package edu.pku;

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
        final Set<Integer> points;

        Item(String type, Set<Integer> points) {
            this.type = type;
            this.points = points;
        }

        @Override
        public String toString() {
            return points.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return Objects.equals(type, item.type) &&
                    Objects.equals(points, item.points);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, points);
        }
    }

    public static final String ALLOC = "<benchmark.internal.BenchmarkN: void alloc(int)>";
    public static final String TEST = "<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>";
    public static final String THIS = "";
    private static final TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<>();
    private static final Map<String, Integer> static2Id = new HashMap<>();         // className.field -> allocId
    private static final Map<String, Integer> implicit2Id = new HashMap<>();       // className.method.unit -> allocId

    public static void addResultsToAllocId(int id, TreeSet<Integer> points) {
        if (points == null) return;
        result.computeIfAbsent(id, k -> new TreeSet<>());
        result.get(id).addAll(points);
    }

    public static TreeMap<Integer, TreeSet<Integer>> getResults() {
        return result;
    }

    public static Map<String, Integer> getStaticAllocIds() {
        return static2Id;
    }

    public static Map<String, Integer> getImplicitAllocIds() {
        return implicit2Id;
    }

    public static MemoryManager merge(MemoryManager m1, MemoryManager m2) {
        MemoryManager m = m1.getCopy();
        m.nextAllocId = Math.min(m1.nextAllocId, m2.nextAllocId);
        for (Integer allocId : m2.id2f2s.keySet()) {
            Map<String, Item> map1 = m.id2f2s.computeIfAbsent(allocId, k -> new HashMap<>());
            Map<String, Item> map2 = m2.id2f2s.get(allocId);
            for (Map.Entry<String, Item> entry : map2.entrySet()) {
                map1.putIfAbsent(
                        entry.getKey(),
                        new Item(entry.getValue().type, new TreeSet<>())
                );
                map1.get(entry.getKey()).points.addAll(entry.getValue().points);
            }
        }
        return m;
    }

    private Map<Integer, Map<String, Item>> id2f2s = new HashMap<>(); // allocId -> field -> pointSet
    private Map<Integer, String> id2type = new HashMap<>();           // allocId -> type
    private int nextAllocId = -1;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryManager that = (MemoryManager) o;
        return nextAllocId == that.nextAllocId &&
                Objects.equals(id2f2s, that.id2f2s) &&
                Objects.equals(id2type, that.id2type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id2f2s, id2type, static2Id, implicit2Id, nextAllocId);
    }

    public MemoryManager getCopy() {
        MemoryManager copy = new MemoryManager();
        copy.id2f2s = new HashMap<>();
        for (Integer key : id2f2s.keySet()) {
            Map<String, Item> map = id2f2s.get(key);
            copy.id2f2s.put(key, new HashMap<>());
            for (String key2 : map.keySet()) {
                copy.id2f2s.get(key).put(key2, new Item(
                        map.get(key2).type,
                        new TreeSet<>(map.get(key2).points)
                ));
            }
        }
        copy.id2type = new HashMap<>(id2type);
        copy.nextAllocId = nextAllocId;
        return copy;
    }

    public int extractTest(InvokeExpr ie) {
        if (ie.getMethod().toString().equals(TEST)) {
            int id = ((IntConstant) ie.getArgs().get(0)).value;
            addResultsToAllocId(id, new TreeSet<>());
            return id;
        }
        return 0;
    }

    public int extractAlloc(InvokeExpr ie) {
        if (ie.getMethod().toString().equals(ALLOC)) {
            return ((IntConstant) ie.getArgs().get(0)).value;
        }
        return 0;
    }

    public int initStaticAllocId(String className, String fieldName, SootClass fieldClass) {
        String key = className + "." + fieldName;
        int id;
        if (static2Id.containsKey(key)) {
            id = static2Id.get(key);
            if (!id2f2s.containsKey(id)) initAllocId(id, fieldClass);
            return id;
        }
        id = nextAllocId--;
        static2Id.put(key, id);
        initAllocId(id, fieldClass);
        return id;
    }

    public int initImplicitAllocId(SootMethod method, Unit u, SootClass allocClass) {
        String key = method.getDeclaringClass().getName() + "." + method.getName() + "." + u.toString();
        int id;
        if (implicit2Id.containsKey(key)) {
            id = implicit2Id.get(key);
            if (!id2f2s.containsKey(id)) initAllocId(id, allocClass);
            return id;
        }
        id = nextAllocId--;
        implicit2Id.put(key, id);
        initAllocId(id, allocClass);
        return id;
    }

    public void initAllocId(int id, SootClass c) {
        id2type.put(id, c.getName());
        id2f2s.put(id, new HashMap<>());
        id2f2s.get(id).put(THIS, new Item(c.getName(), new TreeSet<>()));
        id2f2s.get(id).get(THIS).points.add(id);
        for (SootField field : c.getFields()) {
            id2f2s.get(id).put(field.getName(), new Item(field.getType().toString(), new TreeSet<>()));
        }/*
        while (c.hasSuperclass()) {
            c = c.getSuperclass();
            //if (c.isJavaLibraryClass()) break;
            for (SootField field : c.getFields()) {
                id2f2s.get(id).putIfAbsent(field.getName(), new Item(field.getType().toString(), new TreeSet<>()));
            }
        }*/
    }

    public Set<Integer> getPointToSet(int id, String field) {
        return id2f2s.get(id).get(field).points;
    }

    public Set<Integer> getPointToSet(Collection<Integer> ids, String field) {
        TreeSet<Integer> r = new TreeSet<>();
        for (Integer id : ids) {
            r.addAll(getPointToSet(id, field));
        }
        return r;
    }

    public void updatePointToSet(int id, String field, Collection<Integer> points) {
        getPointToSet(id, field).clear();
        getPointToSet(id, field).addAll(points);
    }

    public void updatePointToSet(Collection<Integer> ids, String field, Collection<Integer> points) {
        if (ids.size() == 1) { // Strong Update
            updatePointToSet(ids.iterator().next(), field, points);
        } else { // Weak Update
            for (Integer id : ids) {
                getPointToSet(id, field).addAll(points);
            }
        }
    }

    public Map<Integer, Map<String, Item>> getMemAllocTable() {
        return id2f2s;
    }
}

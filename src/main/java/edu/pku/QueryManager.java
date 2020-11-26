package edu.pku;

import java.util.TreeMap;
import java.util.TreeSet;

public class QueryManager {
    private static final TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<>();

    public static void addResults(int id, TreeSet<Integer> points) {
        if (points == null) return;
        result.computeIfAbsent(id, k -> new TreeSet<>());
        result.get(id).addAll(points);
    }

    public static TreeMap<Integer, TreeSet<Integer>> getResult() {
        return result;
    }
}

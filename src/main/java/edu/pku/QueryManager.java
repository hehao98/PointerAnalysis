package edu.pku;

import soot.Local;

import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class QueryManager {
    static final TreeMap<Integer, List<Local>> queries = new TreeMap<>();
    static final TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<>();

    public static void addResults(int id, TreeSet<Integer> points) {
        if (points == null) return;
        result.computeIfAbsent(id, k -> new TreeSet<>());
        result.get(id).addAll(points);
    }
}

package edu.pku;

import java.util.*;

public class Anderson {

    private int currAllocId = 0;
    private final Map<String, TreeSet<Integer>> pts = new HashMap<>();

    public void add(String key, Integer point) {
        pts.get(key).add(point);
    }

    public void merge(String from, String to) {
        pts.get(from).addAll(pts.get(to));
    }

    public void replace(String from, String to) {
        pts.get(from).clear();
        pts.get(from).addAll(pts.get(to));
    }

    public void clear() {
        pts.clear();
    }

    public void mergeAll(String key, Collection<Integer> pointTos) {
        pts.computeIfAbsent(key, k -> new TreeSet<>()).addAll(pointTos);
    }

    public void mergeAll(Anderson another) {
        for (Map.Entry<String, TreeSet<Integer>> entry: another.getPointToSet().entrySet()) {
            mergeAll(entry.getKey(), entry.getValue());
        }
    }

    public int getCurrAllocId() {
        return currAllocId;
    }

    public void setCurrAllocId(int currAllocId) {
        this.currAllocId = currAllocId;
    }

    public Map<String, TreeSet<Integer>> getPointToSet() {
        return pts;
    }

    public TreeSet<Integer> getPointToSet(String key) {
        pts.computeIfAbsent(key, k -> new TreeSet<>());
        return pts.get(key);
    }

}

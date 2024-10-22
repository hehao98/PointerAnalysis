package edu.pku;

import java.util.*;

public class Anderson {

    private int currAllocId = 0;
    private MemoryManager memory = new MemoryManager();
    private final Map<String, TreeSet<Integer>> pts = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Anderson anderson = (Anderson) o;
        return Objects.equals(pts, anderson.pts) && Objects.equals(memory, anderson.memory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pts, memory);
    }

    public void add(String key, Integer point) {
        pts.computeIfAbsent(key, k -> new TreeSet<>()).add(point);
    }

    public void merge(String from, String to) {
        pts.computeIfAbsent(to, k -> new TreeSet<>()).addAll(pts.computeIfAbsent(from, k -> new TreeSet<>()));
    }

    public void replace(String from, String to) {
        pts.computeIfAbsent(to, k -> new TreeSet<>()).clear();
        pts.get(to).addAll(pts.computeIfAbsent(from, k -> new TreeSet<>()));
    }

    public void replaceWith(String key, Collection<Integer> points) {
        pts.computeIfAbsent(key, k -> new TreeSet<>()).clear();
        pts.get(key).addAll(points);
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

    public MemoryManager getMemory() {
        return memory;
    }

    public void setMemory(MemoryManager memory) {
        this.memory = memory;
    }

    public Map<String, TreeSet<Integer>> getPointToSet() {
        return pts;
    }

    public TreeSet<Integer> getPointToSet(String key) {
        pts.computeIfAbsent(key, k -> new TreeSet<>());
        return pts.get(key);
    }
}

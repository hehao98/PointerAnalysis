package edu.pku;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MemoryManager {
    private static final Set<Integer> allocIds = new TreeSet<>();
    private static final Map<String, Integer> staticAllocIds = new HashMap<>();
    private static int nextAllocId = -1;

    public static Set<Integer> getAllocIds() {
        return allocIds;
    }

    public static int getNextAllocId() {
        return nextAllocId--;
    }

    public static void addExplicitAllocId(int id) {
        allocIds.add(id);
    }

    public static int putStaticAllocId(String className, String fieldName) {
        String key = className + "." + fieldName;
        if (!staticAllocIds.containsKey(key))
            staticAllocIds.put(key, nextAllocId--);
        return staticAllocIds.get(key);
    }
}

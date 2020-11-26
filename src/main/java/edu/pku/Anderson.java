package edu.pku;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Anderson {

    private static final Logger LOG = LoggerFactory.getLogger(Anderson.class);

    public static class AssignConstraint {
        String from, to;

        AssignConstraint(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return "AssignConstraint{" +
                    "from=" + from +
                    ", to=" + to +
                    '}';
        }
    }

    public static class AssignFromHeapConstraint {
        String from;
        String to;
        String field; // field name, "" if not a field reference

        AssignFromHeapConstraint(String from, String to, String field) {
            this.from = from;
            this.to = to;
            this.field = field;
        }

        @Override
        public String toString() {
            return "AssignConstraint{" +
                    "from=" + from +
                    (field == null ? "" : ("." + field)) +
                    ", to=" + to +
                    '}';
        }
    }

    public static class AssignToHeapConstraint {
        String from;
        String to;
        String field; // field name, "" if not a field reference

        AssignToHeapConstraint(String from, String to, String field) {
            this.from = from;
            this.to = to;
            this.field = field;
        }

        @Override
        public String toString() {
            return "AssignConstraint{" +
                    "from=" + from +
                    ", to=" + to +
                    (field == null ? "" : ("." + field)) +
                    '}';
        }
    }

    public static class NewConstraint {
        String to;
        int allocId;

        NewConstraint(int allocId, String to) {
            this.allocId = allocId;
            this.to = to;
        }

        @Override
        public String toString() {
            return "NewConstraint{" +
                    "to=" + to +
                    ", allocId=" + allocId +
                    '}';
        }
    }

    private int currAllocId = 0;
    private final Map<Integer, Map<String, TreeSet<Integer>>> id2f2s = new HashMap<>(); // (allocId -> field -> pointSet)
    private final List<AssignConstraint> assignConstraints = new ArrayList<>();
    private final List<AssignFromHeapConstraint> assignFromHeapConstraints = new ArrayList<>();
    private final List<AssignToHeapConstraint> assignToHeapConstraints = new ArrayList<>();
    private final List<NewConstraint> newConstraints = new ArrayList<>();
    private final Map<String, TreeSet<Integer>> pts = new HashMap<>();

    public void addAssignConstraint(String from, String to) {
        assignConstraints.add(new AssignConstraint(from, to));
    }

    public void addAssignFromHeapConstraint(String from, String to, String field) {
        assignFromHeapConstraints.add(new AssignFromHeapConstraint(from, to, field));
    }

    public void addAssignToHeapConstraint(String from, String to, String field) {
        assignToHeapConstraints.add(new AssignToHeapConstraint(from, to, field));
    }

    public void addNewConstraint(int alloc, String to) {
        newConstraints.add(new NewConstraint(alloc, to));
    }

    public void run() {
        LOG.info("Anderson algorithm running...");
        LOG.info("newConstraints = {}", newConstraints);
        LOG.info("assignConstraints = {}", assignConstraints);
        LOG.info("assignFromHeapConstraints = {}", assignFromHeapConstraints);
        LOG.info("assignToHeapConstraints = {}", assignToHeapConstraints);
        for (NewConstraint nc : newConstraints) {
            if (!pts.containsKey(nc.to)) {
                pts.put(nc.to, new TreeSet<>());
            }
            pts.get(nc.to).add(nc.allocId);
        }
        for (boolean flag = true; flag; ) {
            flag = false;
            for (AssignConstraint ac : assignConstraints) {
                if (!pts.containsKey(ac.from)) continue;
                if (!pts.containsKey(ac.to)) pts.put(ac.to, new TreeSet<>());
                if (pts.get(ac.to).addAll(pts.get(ac.from))) flag = true;

            }
            for (AssignFromHeapConstraint ac : assignFromHeapConstraints) {
                if (!pts.containsKey(ac.from)) continue;
                if (!pts.containsKey(ac.to)) pts.put(ac.to, new TreeSet<>());
                TreeSet<Integer> toAdd = new TreeSet<>();
                for (int allocId : pts.get(ac.from)) {
                    if (!id2f2s.containsKey(allocId)) continue;
                    if (id2f2s.get(allocId).containsKey(ac.field)) {
                        toAdd.addAll(id2f2s.get(allocId).get(ac.field));
                    }
                }
                if (pts.get(ac.to).addAll(toAdd)) flag = true;
            }
            for (AssignToHeapConstraint ac : assignToHeapConstraints) {
                if (!pts.containsKey(ac.to)) continue;
                if (!pts.containsKey(ac.from)) continue;
                for (Integer allocId : pts.get(ac.to)) {
                    id2f2s.putIfAbsent(allocId, new TreeMap<>());
                    id2f2s.get(allocId).putIfAbsent(ac.field, new TreeSet<>());
                    if (id2f2s.get(allocId).get(ac.field).addAll(pts.get(ac.from))) flag = true;
                }
            }
        }
        LOG.info("Solved point2Set = {}", pts);
        LOG.info("Solved allocId2field2Set = {}", id2f2s);
    }

    public TreeSet<Integer> getPointsToSet(String value) {
        if (pts.containsKey(value))
            return pts.get(value);
        return new TreeSet<>();
    }

    public void addAllTo(String value, Collection<Integer> pointTos) {
        pts.computeIfAbsent(value, k -> new TreeSet<>()).addAll(pointTos);
    }

    public int getCurrAllocId() {
        return currAllocId;
    }

    public void setCurrAllocId(int currAllocId) {
        this.currAllocId = currAllocId;
    }

    public void clear() {
        id2f2s.clear();
        pts.clear();
        newConstraints.clear();
        assignConstraints.clear();
        assignToHeapConstraints.clear();
        assignFromHeapConstraints.clear();
    }

    public void addAllFrom(Anderson another) {
        currAllocId = another.currAllocId;
        addHeapFrom(another);
        for (String key : another.pts.keySet()) {
            if (pts.containsKey(key)) {
                pts.get(key).addAll(another.pts.get(key));
            } else {
                pts.put(key, new TreeSet<>(another.pts.get(key)));
            }
        }
        newConstraints.addAll(another.newConstraints);
        assignConstraints.addAll(another.assignConstraints);
        assignToHeapConstraints.addAll(another.assignToHeapConstraints);
        assignFromHeapConstraints.addAll(another.assignFromHeapConstraints);
    }

    public void addHeapFrom(Anderson another) {
        for (Integer id : another.id2f2s.keySet()) {
            if (id2f2s.containsKey(id)) {
                Map<String, TreeSet<Integer>> map = id2f2s.get(id);
                Map<String, TreeSet<Integer>> anotherMap = another.id2f2s.get(id);
                for (String key : anotherMap.keySet()) {
                    if (map.containsKey(key)) {
                        map.get(key).addAll(anotherMap.get(key));
                    } else {
                        map.put(key, new TreeSet<>(anotherMap.get(key)));
                    }
                }
            } else {
                id2f2s.put(id, new HashMap<>(another.id2f2s.get(id)));
            }
        }
    }
}

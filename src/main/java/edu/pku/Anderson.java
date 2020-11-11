package edu.pku;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Local;

public class Anderson {

    private static final Logger LOG = LoggerFactory.getLogger(Anderson.class);

    public static class AssignConstraint {
        Local from, to;

        AssignConstraint(Local from, Local to) {
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
        Local from;
        Local to;
        String field; // field name, null if not a field reference

        AssignFromHeapConstraint(Local from, Local to, String field) {
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
        Local from;
        Local to;
        String field; // field name, null if not a field reference

        AssignToHeapConstraint(Local from, Local to, String field) {
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
        Local to;
        int allocId;

        NewConstraint(int allocId, Local to) {
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

    private final List<AssignConstraint> assignConstraints = new ArrayList<>();
    private final List<AssignFromHeapConstraint> assignFromHeapConstraints = new ArrayList<>();
    private final List<AssignToHeapConstraint> assignToHeapConstraints = new ArrayList<>();
    private final List<NewConstraint> newConstraints = new ArrayList<>();
    private final Map<Local, TreeSet<Integer>> pts = new HashMap<>();
    private final Map<Integer, Map<String, TreeSet<Integer>>> id2f2s = new HashMap<>(); // (allocId -> field -> pointSet)

    public void addAssignConstraint(Local from, Local to) {
        assignConstraints.add(new AssignConstraint(from, to));
    }

    public void addAssignFromHeapConstraint(Local from, Local to, String field) {
        assignFromHeapConstraints.add(new AssignFromHeapConstraint(from, to, field));
    }

    public void addAssignToHeapConstraint(Local from, Local to, String field) {
        assignToHeapConstraints.add(new AssignToHeapConstraint(from, to, field));
    }

    public void addNewConstraint(int alloc, Local to) {
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

    public TreeSet<Integer> getPointsToSet(Local local) {
        return pts.get(local);
    }
}

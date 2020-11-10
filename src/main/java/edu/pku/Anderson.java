package edu.pku;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
        String name; // field name, null if not a field reference

        AssignFromHeapConstraint(Local from, Local to, String name) {
            this.from = from;
            this.to = to;
            this.name = name;
        }

        @Override
        public String toString() {
            return "AssignConstraint{" +
                    "from=" + from +
                    (name == null ? "" : ("." + name)) +
                    ", to=" + to +
                    '}';
        }
    }

    public static class AssignToHeapConstraint {
        Local from;
        Local to;
        String name; // field name, null if not a field reference

        AssignToHeapConstraint(Local from, Local to, String name) {
            this.from = from;
            this.to = to;
            this.name = name;
        }

        @Override
        public String toString() {
            return "AssignConstraint{" +
                    "from=" + from +
                    ", to=" + to +
                    (name == null ? "" : ("." + name)) +
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
    private final List<NewConstraint> newConstraintList = new ArrayList<>();
    Map<Local, TreeSet<Integer>> pts = new HashMap<>();

    void addAssignConstraint(Local from, Local to) {
        assignConstraints.add(new AssignConstraint(from, to));
    }

    void addAssignFromHeapConstraint(Local from, Local to, String field) {
        assignFromHeapConstraints.add(new AssignFromHeapConstraint(from, to, field));
    }

    void addAssignToHeapConstraint(Local from, Local to, String field) {
        assignToHeapConstraints.add(new AssignToHeapConstraint(from, to, field));
    }

    void addNewConstraint(int alloc, Local to) {
        newConstraintList.add(new NewConstraint(alloc, to));
    }

    void run() {
        LOG.info("Anderson running on newConstraints = {}", newConstraintList);
        LOG.info("Anderson running on assignConstraints = {}", assignConstraints);
        for (NewConstraint nc : newConstraintList) {
            if (!pts.containsKey(nc.to)) {
                pts.put(nc.to, new TreeSet<>());
            }
            pts.get(nc.to).add(nc.allocId);
        }
        for (boolean flag = true; flag; ) {
            flag = false;
            for (AssignConstraint ac : assignConstraints) {
                if (!pts.containsKey(ac.from)) {
                    continue;
                }
                if (!pts.containsKey(ac.to)) {
                    pts.put(ac.to, new TreeSet<>());
                }
                if (pts.get(ac.to).addAll(pts.get(ac.from))) {
                    flag = true;
                }
            }
            // TODO solve constraints here
            //for AssignFromHeapConstraint ac
        }
        LOG.info("Solved point2Set = {}", pts);
    }

    TreeSet<Integer> getPointsToSet(Local local) {
        return pts.get(local);
    }
}

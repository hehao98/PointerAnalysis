package edu.pku;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import soot.Local;

public class Anderson {
    public static class AssignConstraint {
        Local from, to;

        AssignConstraint(Local from, Local to) {
            this.from = from;
            this.to = to;
        }
    }

    public static class NewConstraint {
        Local to;
        int allocId;

        NewConstraint(int allocId, Local to) {
            this.allocId = allocId;
            this.to = to;
        }
    }

    private final List<AssignConstraint> assignConstraintList = new ArrayList<>();
    private final List<NewConstraint> newConstraintList = new ArrayList<>();
    Map<Local, TreeSet<Integer>> pts = new HashMap<>();

    void addAssignConstraint(Local from, Local to) {
        assignConstraintList.add(new AssignConstraint(from, to));
    }

    void addNewConstraint(int alloc, Local to) {
        newConstraintList.add(new NewConstraint(alloc, to));
    }

    void run() {
        for (NewConstraint nc : newConstraintList) {
            if (!pts.containsKey(nc.to)) {
                pts.put(nc.to, new TreeSet<Integer>());
            }
            pts.get(nc.to).add(nc.allocId);
        }
        for (boolean flag = true; flag; ) {
            flag = false;
            for (AssignConstraint ac : assignConstraintList) {
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
        }
    }

    TreeSet<Integer> getPointsToSet(Local local) {
        return pts.get(local);
    }

}

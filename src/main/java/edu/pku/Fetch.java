package edu.pku;

import soot.*;
import soot.options.*;

import java.util.*;

public class Fetch {

    public static void main(String[] args) {
        Scene.v().setSootClassPath("/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/rt.jar");
        Scene.v().extendSootClassPath("/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/jre/lib/jce.jar");
        Scene.v().extendSootClassPath("sootOutput/");

        Options.v().set_verbose(true);

        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.myTransform", new SceneTransformer() {
                    protected void internalTransform(String phaseName,
                                                     Map options) {
                        System.out.println(phaseName);
                        System.out.println(Scene.v().getApplicationClasses());
                    }
                }));

        Options.v().set_whole_program(true);

        PackManager.v().getPack("jap").add(
                new Transform("jap.myTransform", new BodyTransformer() {

                    protected void internalTransform(Body body, String phase, Map options) {
                        for (Unit u : body.getUnits()) {
                            System.out.print(u);
                            System.out.print(": ");
                            System.out.println(u.getTags());
                        }
                    }
                }));

        PhaseOptions.v().setPhaseOption("jap.npc", "on");
        soot.Main.main(args);
    }
}


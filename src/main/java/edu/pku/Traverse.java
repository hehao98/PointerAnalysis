package edu.pku;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.util.Chain;

import java.util.List;

public class Traverse {

    public static void main2(String[] args) {
        Scene.v().setSootClassPath("/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home/jre/lib/rt.jar");
        Scene.v().extendSootClassPath("sootOutput");
        SootClass sClass = Scene.v().loadClassAndSupport("HelloWorld");
        Scene.v().loadNecessaryClasses();

        List<SootMethod> sMethods = sClass.getMethods();
        Chain<SootField> sFields = sClass.getFields();

        for (SootField f : sFields) {
            System.out.println(f.getDeclaration());
        }

        for (SootMethod m : sMethods) {
            System.out.println(m.getDeclaration());
        }
    }
}

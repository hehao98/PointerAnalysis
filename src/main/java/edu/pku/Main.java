package edu.pku;

import soot.*;
import soot.jimple.*;
import soot.util.*;
import soot.options.Options;

import java.io.*;
import java.util.*;


public class Main {
    public static void main2(String[] args) throws IOException {
        // Modify this line to a rt.jar, typically in /path/to/JDK/jre/lib/rt.jar
        Scene.v().setSootClassPath("/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home/jre/lib/rt.jar");
        Scene.v().loadClassAndSupport("java.lang.Object");
        Scene.v().loadClassAndSupport("java.lang.System");
        SootClass sClass = new SootClass("HelloWorld", Modifier.PUBLIC);
        sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(sClass);
        SootMethod sMethod = new SootMethod("main",
                Arrays.asList(new Type[]{ArrayType.v(RefType.v("java.lang.String"), 1)}),
                VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        sClass.addMethod(sMethod);

        {
            JimpleBody jBody = Jimple.v().newBody(sMethod);
            sMethod.setActiveBody(jBody);
            Chain units = jBody.getUnits();

            Local jArg = Jimple.v().newLocal("l0", ArrayType.v(RefType.v("java.lang.String"), 1));
            jBody.getLocals().add(jArg);

            Local jTmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
            jBody.getLocals().add(jTmpRef);

            // l0 = @parameter0
            units.add(Jimple.v().newIdentityStmt(jArg,
                    Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0)));

            // tmpRef = java.lang.System.out
            units.add(Jimple.v().newAssignStmt(jTmpRef,
                    Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));

            // tmpRef.println("Hello World!")
            {
                SootMethod sToCall = Scene.v().getMethod("<java.io.PrintStream: void println(java.lang.String)>");
                units.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(jTmpRef, sToCall.makeRef(), StringConstant.v("Hello World!"))));
            }

            // return
            units.add(Jimple.v().newReturnVoidStmt());
        }

        String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_class);
        OutputStream streamOut = new JasminOutputStream(
                new FileOutputStream(fileName));
        PrintWriter writerOut = new PrintWriter(
                new OutputStreamWriter(streamOut));

        JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
        jasminClass.print(writerOut);
        writerOut.flush();
        streamOut.close();
    }
}
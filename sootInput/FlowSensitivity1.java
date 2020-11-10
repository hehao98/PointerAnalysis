package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

/*
 * @testcase FlowSensitivity1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Is the analysis flow-sensitive?
 */
public class FlowSensitivity1 {

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        A a = new A();
        BenchmarkN.alloc(2);
        A b = new A();

        BenchmarkN.test(1, b);

        // This test case will not work because soot will assign a new local variable for b in Jimple
        b = a;
        B x = b.getF();
        B y = a.getF();

        BenchmarkN.test(2, b);
    }
}

package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

/*
 * @testcase FieldSensitivity2
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Field Sensitivity without static method
 */
public class FieldSensitivity2 {

    public static void main(String[] args) {
        BenchmarkN.alloc(5);
        B b = new B();
        BenchmarkN.alloc(6);
        B c = new B();
        BenchmarkN.alloc(7);
        A a1 = new A();
        BenchmarkN.alloc(8);
        A a2 = new A();
        a1.f = b;
        a2.f = c;
        BenchmarkN.test(1, a1.f);
        BenchmarkN.test(2, a2.f);
        b = a2.f;
        BenchmarkN.test(3, b);
    }

}

package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class FlowSensitivity2 {

    public static A x;

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        A a = new A();
        BenchmarkN.alloc(2);
        A b = new A();

        FlowSensitivity2.x = a;
        BenchmarkN.test(1, FlowSensitivity2.x);

        FlowSensitivity2.x = b;
        BenchmarkN.test(2, FlowSensitivity2.x);
    }

}

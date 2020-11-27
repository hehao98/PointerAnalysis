package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class FlowSensitivity2 {

    public static B x;

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        B a = new B();
        BenchmarkN.alloc(2);
        B b = new B();

        x = a;
        BenchmarkN.test(1, x);

        x = b;
        BenchmarkN.test(2, x);
    }

}

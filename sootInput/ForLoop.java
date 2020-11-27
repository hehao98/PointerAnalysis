package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class ForLoop {

    public static void main(String[] args) {
        B b;
        for (int i = 0; i < 10; ++i) {
            b = new B();
            BenchmarkN.test(1, b);
        }
    }
}

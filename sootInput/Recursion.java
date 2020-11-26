package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class Recursion {
    public static void f(int x) {
        if (x == 0) return;
        BenchmarkN.alloc(1);
        B b = new B();
        BenchmarkN.test(1, b);
        f(x - 1);
    }

    public static void main(String[] args) {
        f(3);
    }

}
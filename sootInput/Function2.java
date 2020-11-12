package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

public class Function2 {
    public static void assign(A a, A b) {
        a.f = b.f;
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        B b1 = new B();
        BenchmarkN.alloc(2);
        A a1 = new A(b1);
        BenchmarkN.alloc(3);
        A a2 = new A();
        assign(a2, a1);
        BenchmarkN.test(1, a2.f);
    }

}
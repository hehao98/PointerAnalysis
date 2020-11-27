package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

public class RecursionComplex {

    public static void bar(B k) {
        BenchmarkN.alloc(7);
        A d = new A(k);
        foo(d);
        BenchmarkN.test(8, d);
    }

    public static void foo(A k) {
        BenchmarkN.alloc(4);
        A d = new A();
        BenchmarkN.alloc(5);
        A s = new A();
        BenchmarkN.alloc(6);
        B b = new B();
        k.f = b;
        bar(b);
        BenchmarkN.test(5, d);
        BenchmarkN.test(6, s.f);
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        A a = new A();
        BenchmarkN.alloc(2);
        A b = new A();
        BenchmarkN.alloc(3);
        A c = new A();
        BenchmarkN.test(1, a);
        if (args.length > 1) a = b;
        if (args.length > 2) b = c;
        BenchmarkN.test(2, a);
        foo(c);
        BenchmarkN.test(3, c);
        BenchmarkN.test(4, b);
        BenchmarkN.test(7, c.f);
    }

}

package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class WeakUpdate {

    public static class A {
        public B f;
    }

    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new A();
        A a;
        BenchmarkN.alloc(1);
        B b1 = new B();
        BenchmarkN.alloc(2);
        B b2 = new B();
        BenchmarkN.alloc(3);
        B b3 = new B();
        a1.f = b1;
        a2.f = b2;

        if (args.length > 0) {
            a = a1;
        } else {
            a = a2;
        }
        a.f = b3;
        BenchmarkN.test(1, a1.f);
        BenchmarkN.test(2, a2.f);
    }
}

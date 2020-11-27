package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class ForLoopComplex {

    public static class A {
        public B f;
    }

    public static void main(String[] args) {
        A a = new A();
        BenchmarkN.alloc(1);
        B b1 = new B();
        BenchmarkN.alloc(2);
        B b2 = new B();
        BenchmarkN.alloc(3);
        B b3 = new B();
        a.f = b3;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].length() > 3)
                a.f = b1;
            else
                a.f = b2;
        }
        BenchmarkN.test(1, a.f);
    }
}

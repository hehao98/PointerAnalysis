package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class FinalTest {

    public static class A {
        public B f;
        public A a;
    }

    public static B x;
    public static B y;
    public static A a;

    public static B f1(B b, int i) {
        if (i % 3 == 0) {
            BenchmarkN.alloc(6);
            B c = new B();
            x = c;
            a.a.f = c;
            return c;
        }
        return b;
    }

    public static B f2(B b) {
        BenchmarkN.alloc(7);
        B c = new B();
        x = c;
        a.a.f = c;
        return c;
    }

    public static A bar(String[] args, A a1, A a2) {
        if (args.length > 0) {
            return a1;
        }
        return a2;
    }

    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new A();
        BenchmarkN.alloc(1);
        B b1 = new B();
        BenchmarkN.alloc(2);
        B b2 = new B();
        BenchmarkN.alloc(3);
        B b3 = new B();
        a1.f = b1;
        a2.f = b2;

        a = bar(args, a1, a2);
        a.f = b3;
        BenchmarkN.test(1, a1.f);
        BenchmarkN.test(2, a2.f);

        for (int i = 0; i < args.length; ++i) {
            if (i % 2 == 0) {
                x = b2;
                y = b3;
                BenchmarkN.test(3, x);
                BenchmarkN.test(4, y);
                a.f = f1(x, i);
                BenchmarkN.test(5, a1.f);
            } else {
                x = b3;
                y = b2;
                BenchmarkN.test(6, x);
                BenchmarkN.test(7, y);
                a.f = f2(y);
                BenchmarkN.test(8, a1.f);
            }
        }
        BenchmarkN.test(9, a.f);
        BenchmarkN.test(10, x);
        BenchmarkN.test(11, y);
        BenchmarkN.test(12, a.a.f);
    }
}

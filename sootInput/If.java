package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class If {

    public static B b;

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
        if (args[1].length() > 3) {
            A a2 = new A();
            a2.f = b1;
            a = a2;
        } else if (args[1].length() < 1) {
            A a3 = new A();
            a3.f = b2;
            a = a3;
        }
        BenchmarkN.test(1, a.f);

        if (args[0].length() > 1) {
            If.b = b1;
        } else {
            If.b = b2;
        }
        BenchmarkN.test(2, If.b);
    }
}

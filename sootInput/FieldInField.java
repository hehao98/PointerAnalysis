package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class FieldInField {

    public static class A {
        public B f;
        public A a;
    }

    public static A a1;
    public static int x;

    public static void main(String[] args) {
        int l = args.length;
        x = l;
        a1 = new A();
        A a2 = new A();
        BenchmarkN.alloc(1);
        B b1 = new B();
        BenchmarkN.alloc(2);
        B b2 = new B();
        a2.f = b1;
        a1.a = a2;
        BenchmarkN.test(1, a1.a.f);
    }
}

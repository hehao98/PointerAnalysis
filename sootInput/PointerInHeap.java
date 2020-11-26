package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class PointerInHeap {

    public static B x;
    public static B y;

    public static void f1() {
        BenchmarkN.alloc(3);
        B c = new B();
        x = c;
        BenchmarkN.test(5, x);
    }

    public static void f2() {
        BenchmarkN.alloc(4);
        B c = new B();
        x = c;
        BenchmarkN.test(6, x);
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        B a = new B();
        BenchmarkN.alloc(2);
        B b = new B();

        x = a;
        y = b;
        BenchmarkN.test(1, x);
        BenchmarkN.test(2, y);

        f1();

        x = b;
        y = a;
        BenchmarkN.test(3, x);
        BenchmarkN.test(4, y);

        f2();
    }

}

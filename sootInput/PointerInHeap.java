package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class PointerInHeap {

    public static A x;
    public static A y;

    public static void f1() {
        BenchmarkN.alloc(3);
        A c = new A();
        x = c;
        BenchmarkN.test(5, x);
    }

    public static void f2() {
        BenchmarkN.alloc(4);
        A c = new A();
        x = c;
        BenchmarkN.test(6, x);
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        A a = new A();
        BenchmarkN.alloc(2);
        A b = new A();

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

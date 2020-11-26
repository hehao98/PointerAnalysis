package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

public class RecursionComplex {

    public static void testme(B k){
        BenchmarkN.alloc(7);
        A d = new A(k);
        specialchange(d);
        BenchmarkN.test(8, d);// 7
    }

    public static void specialchange(A k) {
        BenchmarkN.alloc(4);
        A d = new A();
        BenchmarkN.alloc(5);
        A s = new A();
        BenchmarkN.alloc(6);
        B b = new B();
        k.f = b;
        testme(b);
        BenchmarkN.test(5, d); // 4
        BenchmarkN.test(6, s.f); //0
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        A a = new A();
        BenchmarkN.alloc(2);
        A b = new A();
        BenchmarkN.alloc(3);
        A c = new A();
        BenchmarkN.test(1, a);  // 1
        if (args.length > 1) a = b;
        if (args.length > 2) b = c;
        BenchmarkN.test(2, a);  // 1 2
        specialchange(c);
        BenchmarkN.test(3, c);  // 4
        BenchmarkN.test(4, b);  // 2 3
        BenchmarkN.test(7, c.f); // 0 6
    }

}

package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class Hello2 {

    public static void foo() {
        BenchmarkN.alloc(1);
        A a = new A();
        BenchmarkN.test(1, a);
    }

    public static void bar() {
        BenchmarkN.alloc(2);
        A a = new A();
        BenchmarkN.test(2, a);
    }

    public static void main(String[] args) {
        foo();
        bar();
    }
}

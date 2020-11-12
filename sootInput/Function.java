package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class Function {
    public static B assign(B b) {
        return b;
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        B a = new B();
        BenchmarkN.alloc(2);
        B b = new B();
        a = assign(b);
        BenchmarkN.test(1, a);
    }

}
package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;

public class ImplicitAllocId {

    public static void main(String[] args) {
        A a = new A();
        BenchmarkN.alloc(2);
        A b = new A();
        BenchmarkN.alloc(3);
        A c = new A();
        if (args.length > 1) a = b;
        BenchmarkN.test(1, a);
        BenchmarkN.test(2, c);
    }
}

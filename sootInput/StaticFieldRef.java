package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class StaticFieldRef {

    public static B x;
    public static B y;

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        B b = new B();
        x = b;
        BenchmarkN.test(1, x);

        BenchmarkN.alloc(2);
        y = new B();
        BenchmarkN.test(2, y);
    }

}

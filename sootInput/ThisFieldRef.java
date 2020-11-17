package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.B;

public class ThisFieldRef {

    public B x;
    public B y;

    public void foo() {
        BenchmarkN.alloc(2);
        this.x = new B();
        BenchmarkN.alloc(3);
        this.y = new B();
    }

    public void bar() {
        BenchmarkN.test(1, x);
        BenchmarkN.test(2, y);
    }

    public static void main(String[] args) {
        BenchmarkN.alloc(1);
        ThisFieldRef a = new ThisFieldRef();
        a.foo();
        a.bar();
    }

}

package test;

import benchmark.internal.BenchmarkN;

public class Inheritance {

    public static void main(String[] args) {
        B b = new B();
        b.f();
        B c = new C();
        c.f();
    }
}

class A {

}

class B {
    public void f() {
        BenchmarkN.alloc(1);
        A a = new A();
        BenchmarkN.test(1, a);
    }
}

class C extends B {
    @Override
    public void f() {
        BenchmarkN.alloc(2);
        A a = new A();
        BenchmarkN.test(2, a);
    }
}

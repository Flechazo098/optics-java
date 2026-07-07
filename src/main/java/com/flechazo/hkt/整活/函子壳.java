package com.flechazo.hkt.整活;

public interface 函子壳<某盒子 extends 一阶, 证明 extends 函子壳.型> extends 盒子<证明, 某盒子> {
    interface 型 extends 一阶 {
    }

    static <某盒子 extends 一阶, 证明 extends 型> 函子壳<某盒子, 证明> 拆(盒子<证明, 某盒子> 证明盒) {
        return (函子壳<某盒子, 证明>) 证明盒;
    }
}
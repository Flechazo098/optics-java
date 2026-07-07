package com.flechazo.hkt.整活;

public interface 函子<某盒子 extends 一阶, 证明 extends 函子.型> extends 函子壳<某盒子, 证明> {
    interface 型 extends 函子壳.型 {
    }

    static <某盒子 extends 一阶, 证明 extends 型> 函子<某盒子, 证明> 拆(盒子<证明, 某盒子> 证明盒) {
        return (函子<某盒子, 证明>) 证明盒;
    }

    <内容, 新内容> 盒子<某盒子, 新内容> 映射(功能<? super 内容, ? extends 新内容> 功能, 盒子<某盒子, 内容> 值);
}
package com.flechazo.hkt.整活;

public interface 应用函子<某盒子 extends 一阶, 证明 extends 应用函子.型> extends 函子<某盒子, 证明> {
    interface 型 extends 函子.型 {
    }

    static <某盒子 extends 一阶, 证明 extends 型> 应用函子<某盒子, 证明> 拆(盒子<证明, 某盒子> 证明盒) {
        return (应用函子<某盒子, 证明>) 证明盒;
    }

    <内容> 盒子<某盒子, 内容> 纯(内容 值);

    <内容, 新内容> 盒子<某盒子, 新内容> 运用(
            盒子<某盒子, ? extends 功能<内容, 新内容>> 函数盒,
            盒子<某盒子, 内容> 值);

    default <甲, 乙, 丙> 盒子<某盒子, 丙> 映射二(
            盒子<某盒子, 甲> 甲值,
            盒子<某盒子, 乙> 乙值,
            双功能<? super 甲, ? super 乙, ? extends 丙> 功能) {
        return 运用(映射(甲 -> 乙 -> 功能.用(甲, 乙), 甲值), 乙值);
    }
}
package com.flechazo.hkt.整活;

public interface 单子<某盒子 extends 一阶, 证明 extends 应用函子.型> extends 应用函子<某盒子, 证明> {
    interface 型 extends 应用函子.型 {
    }

    <内容, 新内容> 盒子<某盒子, 新内容> 平绑(
            功能<? super 内容, ? extends 盒子<某盒子, 新内容>> 功能,
            盒子<某盒子, 内容> 值);

    @Override
    default <内容, 新内容> 盒子<某盒子, 新内容> 映射(
            功能<? super 内容, ? extends 新内容> 功能,
            盒子<某盒子, 内容> 值) {
        return 平绑(内容 -> 纯(功能.用(内容)), 值);
    }

    @Override
    default <内容, 新内容> 盒子<某盒子, 新内容> 运用(
            盒子<某盒子, ? extends 功能<内容, 新内容>> 函数盒,
            盒子<某盒子, 内容> 值) {
        return 平绑(函数 -> 映射(函数, 值), 函数盒);
    }
}
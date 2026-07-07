package com.flechazo.hkt.整活;

public interface 选择性<某盒子 extends 一阶, 证明 extends 应用函子.型> extends 应用函子<某盒子, 证明> {
    <内容, 新内容> 盒子<某盒子, 新内容> 选择(
            盒子<某盒子, 要么<内容, 新内容>> 值,
            盒子<某盒子, ? extends 功能<内容, 新内容>> 函数);
}
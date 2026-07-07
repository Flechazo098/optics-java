package com.flechazo.hkt.整活;

public interface 笛卡尔<某盒子 extends 一阶, 证明 extends 函子.型> extends 函子<某盒子, 证明> {
    <内容, 新内容> 盒子<某盒子, 二元组<内容, 新内容>> 积(
            盒子<某盒子, 内容> 左,
            盒子<某盒子, 新内容> 右);
}
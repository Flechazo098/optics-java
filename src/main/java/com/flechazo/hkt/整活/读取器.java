package com.flechazo.hkt.整活;

@FunctionalInterface
public interface 读取器<环境, 值> extends 盒子<读取器.型<环境>, 值> {
    final class 型<环境> implements 一阶 {
        private 型() {
        }
    }

    值 运行(环境 环境);

    default <新值> 读取器<环境, 新值> 映射(功能<? super 值, ? extends 新值> 功能) {
        return 环境 -> 功能.用(运行(环境));
    }

    static <环境, 值> 读取器<环境, 值> 纯(值 值) {
        return 环境 -> 值;
    }

    static <环境, 值> 读取器<环境, 值> 拆(盒子<读取器.型<环境>, 值> 值) {
        return (读取器<环境, 值>) 值;
    }

    static <环境> 单子<读取器.型<环境>, 实例.证> 单子() {
        return (单子<读取器.型<环境>, 实例.证>) (单子<?, ?>) 实例.实例;
    }

    default <新值> 读取器<环境, 新值> 平绑(功能<? super 值, ? extends 读取器<环境, 新值>> 功能) {
        return 环境 -> 功能.用(运行(环境)).运行(环境);
    }

    static <环境> 读取器<环境, 环境> 询问() {
        return 环境 -> 环境;
    }

    default <新环境> 读取器<新环境, 值> 局部(功能<? super 新环境, ? extends 环境> 功能) {
        return 环境 -> 运行(功能.用(环境));
    }

    enum 实例 implements 单子<读取器.型<Object>, 实例.证>, 笛卡尔<读取器.型<Object>, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<读取器.型<Object>, 内容> 纯(内容 值) {
            return 读取器.纯(值);
        }

        @Override
        public <内容, 新内容> 盒子<读取器.型<Object>, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<读取器.型<Object>, 新内容>> 功能,
                盒子<读取器.型<Object>, 内容> 值) {
            读取器<Object, 内容> 读取值 = 读取器.拆(值);
            读取器<Object, 新内容> 读取结果 = 环境 -> 读取器.拆(功能.用(读取值.运行(环境))).运行(环境);
            return 读取结果;
        }

        @Override
        public <内容, 新内容> 盒子<读取器.型<Object>, 新内容> 运用(
                盒子<读取器.型<Object>, ? extends 功能<内容, 新内容>> 函数盒,
                盒子<读取器.型<Object>, 内容> 值) {
            读取器<Object, ? extends 功能<内容, 新内容>> 读取函数 = 读取器.拆(函数盒);
            读取器<Object, 内容> 读取值 = 读取器.拆(值);
            读取器<Object, 新内容> 读取结果 = 环境 -> 读取函数.运行(环境).用(读取值.运行(环境));
            return 读取结果;
        }

        @Override
        public <内容, 新内容> 盒子<读取器.型<Object>, 二元组<内容, 新内容>> 积(
                盒子<读取器.型<Object>, 内容> 左,
                盒子<读取器.型<Object>, 新内容> 右) {
            读取器<Object, 内容> 左值 = 读取器.拆(左);
            读取器<Object, 新内容> 右值 = 读取器.拆(右);
            读取器<Object, 二元组<内容, 新内容>> 读取积 = 环境 -> new 二元组<>(左值.运行(环境), 右值.运行(环境));
            return 读取积;
        }
    }
}
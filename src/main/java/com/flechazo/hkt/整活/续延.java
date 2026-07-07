package com.flechazo.hkt.整活;

@FunctionalInterface
public interface 续延<结果, 值> extends 盒子<续延.型<结果>, 值> {
    final class 型<结果> implements 一阶 {
        private 型() {
        }
    }

    结果 运行(功能<? super 值, ? extends 结果> 回调);

    default <新值> 续延<结果, 新值> 映射(功能<? super 值, ? extends 新值> 功能) {
        return 回调 -> 运行(值 -> 回调.用(功能.用(值)));
    }

    static <结果, 值> 续延<结果, 值> 纯(值 值) {
        return 回调 -> 回调.用(值);
    }

    static <结果, 值> 续延<结果, 值> 拆(盒子<续延.型<结果>, 值> 值) {
        return (续延<结果, 值>) 值;
    }

    static <结果> 单子<续延.型<结果>, 实例.证> 单子() {
        return (单子<续延.型<结果>, 实例.证>) (单子<?, ?>) 实例.实例;
    }

    default <新值> 续延<结果, 新值> 平绑(功能<? super 值, ? extends 续延<结果, 新值>> 功能) {
        return 回调 -> 运行(值 -> 功能.用(值).运行(回调));
    }

    enum 实例 implements 单子<续延.型<Object>, 实例.证>, 笛卡尔<续延.型<Object>, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<续延.型<Object>, 内容> 纯(内容 值) {
            return 续延.纯(值);
        }

        @Override
        public <内容, 新内容> 盒子<续延.型<Object>, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<续延.型<Object>, 新内容>> 功能,
                盒子<续延.型<Object>, 内容> 值) {
            续延<Object, 内容> 续延值 = 续延.拆(值);
            return (续延<Object, 新内容>) 回调 -> 续延值.运行(元素 -> 续延.拆(功能.用(元素)).运行(回调));
        }

        @Override
        public <内容, 新内容> 盒子<续延.型<Object>, 新内容> 运用(
                盒子<续延.型<Object>, ? extends 功能<内容, 新内容>> 函数盒,
                盒子<续延.型<Object>, 内容> 值) {
            续延<Object, ? extends 功能<内容, 新内容>> 续延函数 = 续延.拆(函数盒);
            续延<Object, 内容> 续延值 = 续延.拆(值);
            return (续延<Object, 新内容>) 回调 -> 续延函数.运行(函数值 -> 续延值.运行(元素 -> 回调.用(函数值.用(元素))));
        }

        @Override
        public <内容, 新内容> 盒子<续延.型<Object>, 二元组<内容, 新内容>> 积(
                盒子<续延.型<Object>, 内容> 左,
                盒子<续延.型<Object>, 新内容> 右) {
            续延<Object, 内容> 左续延 = 续延.拆(左);
            续延<Object, 新内容> 右续延 = 续延.拆(右);
            return (续延<Object, 二元组<内容, 新内容>>) 回调 -> 左续延.运行(左值 -> 右续延.运行(右值 -> 回调.用(new 二元组<>(左值, 右值))));
        }
    }
}
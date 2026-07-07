package com.flechazo.hkt.整活;

@FunctionalInterface
public interface 状态<状态类型, 值类型> extends 盒子<状态.型<状态类型>, 值类型> {
    final class 型<状态类型> implements 一阶 {
        private 型() {
        }
    }

    状态结果<状态类型, 值类型> 运行(状态类型 当前);

    default <新值> 状态<状态类型, 新值> 映射(功能<? super 值类型, ? extends 新值> 功能) {
        return 当前 -> {
            状态结果<状态类型, 值类型> 结果 = 运行(当前);
            return new 状态结果<>(功能.用(结果.值()), 结果.新状态());
        };
    }

    static <状态类型, 值类型> 状态<状态类型, 值类型> 纯(值类型 值) {
        return 当前 -> new 状态结果<>(值, 当前);
    }

    static <状态类型, 值类型> 状态<状态类型, 值类型> 拆(盒子<状态.型<状态类型>, 值类型> 值) {
        return (状态<状态类型, 值类型>) 值;
    }

    static <状态类型> 单子<状态.型<状态类型>, 实例.证> 单子() {
        return (单子<状态.型<状态类型>, 实例.证>) (单子<?, ?>) 实例.实例;
    }

    default <新值> 状态<状态类型, 新值> 平绑(功能<? super 值类型, ? extends 状态<状态类型, 新值>> 功能) {
        return 当前 -> {
            状态结果<状态类型, 值类型> 结果 = 运行(当前);
            return 功能.用(结果.值()).运行(结果.新状态());
        };
    }

    static <状态类型> 状态<状态类型, 状态类型> 获取() {
        return 当前 -> new 状态结果<>(当前, 当前);
    }

    static <状态类型> 状态<状态类型, 单位> 设置(状态类型 新状态) {
        return 当前 -> new 状态结果<>(单位.实例, 新状态);
    }

    static <状态类型> 状态<状态类型, 单位> 修改(功能<? super 状态类型, ? extends 状态类型> 功能) {
        return 当前 -> new 状态结果<>(单位.实例, 功能.用(当前));
    }

    record 状态结果<状态类型, 值类型>(值类型 值, 状态类型 新状态) {}

    enum 实例 implements 单子<状态.型<Object>, 实例.证>, 笛卡尔<状态.型<Object>, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<状态.型<Object>, 内容> 纯(内容 值) {
            return 状态.纯(值);
        }

        @Override
        public <内容, 新内容> 盒子<状态.型<Object>, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<状态.型<Object>, 新内容>> 功能,
                盒子<状态.型<Object>, 内容> 值) {
            状态<Object, 内容> 状态值 = 状态.拆(值);
            return (状态<Object, 新内容>) 当前状态 -> {
                var 运行结果 = 状态值.运行(当前状态);
                return 状态.拆(功能.用(运行结果.值())).运行(运行结果.新状态());
            };
        }

        @Override
        public <内容, 新内容> 盒子<状态.型<Object>, 新内容> 运用(
                盒子<状态.型<Object>, ? extends 功能<内容, 新内容>> 函数盒,
                盒子<状态.型<Object>, 内容> 值) {
            状态<Object, ? extends 功能<内容, 新内容>> 状态函数 = 状态.拆(函数盒);
            状态<Object, 内容> 状态值 = 状态.拆(值);
            return (状态<Object, 新内容>) 当前状态 -> {
                var 函数结果 = 状态函数.运行(当前状态);
                var 值结果 = 状态值.运行(函数结果.新状态());
                return new 状态结果<>(函数结果.值().用(值结果.值()), 值结果.新状态());
            };
        }

        @Override
        public <内容, 新内容> 盒子<状态.型<Object>, 二元组<内容, 新内容>> 积(
                盒子<状态.型<Object>, 内容> 左,
                盒子<状态.型<Object>, 新内容> 右) {
            状态<Object, 内容> 左状态 = 状态.拆(左);
            状态<Object, 新内容> 右状态 = 状态.拆(右);
            return (状态<Object, 二元组<内容, 新内容>>) 当前状态 -> {
                var 左结果 = 左状态.运行(当前状态);
                var 右结果 = 右状态.运行(左结果.新状态());
                return new 状态结果<>(new 二元组<>(左结果.值(), 右结果.值()), 右结果.新状态());
            };
        }
    }
}
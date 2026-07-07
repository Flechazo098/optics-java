package com.flechazo.hkt.整活;

import java.util.NoSuchElementException;

public sealed interface 也许<内容> extends 盒子<也许.型, 内容> permits 也许.有, 也许.无 {
    final class 型 implements 一阶 {
        private 型() {
        }
    }

    static <内容> 也许<内容> 有(内容 值) {
        return new 有<>(值);
    }

    static <内容> 也许<内容> 无() {
        return (也许<内容>) 无.实例;
    }

    static <内容> 也许<内容> 拆(盒子<也许.型, 内容> 值) {
        return (也许<内容>) 值;
    }

    static 实例 实例() {
        return 实例.实例;
    }

    static 应用函子<也许.型, 实例.证> 应用函子() {
        return 实例.实例;
    }

    static 单子<也许.型, 实例.证> 单子() {
        return 实例.实例;
    }

    static 选择性<也许.型, 实例.证> 选择性() {
        return 实例.实例;
    }

    boolean 有值();

    default boolean 无值() {
        return !有值();
    }

    内容 取();

    default <新内容> 也许<新内容> 映射(功能<? super 内容, ? extends 新内容> 功能) {
        return 有值() ? 有(功能.用(取())) : 无();
    }

    default <新内容> 也许<新内容> 平绑(功能<? super 内容, ? extends 也许<新内容>> 功能) {
        return 有值() ? 功能.用(取()) : 无();
    }

    default 内容 或默认(内容 默认值) {
        return 有值() ? 取() : 默认值;
    }

    record 有<内容>(内容 值) implements 也许<内容> {
        @Override
        public boolean 有值() {
            return true;
        }

        @Override
        public 内容 取() {
            return 值;
        }
    }

    enum 无 implements 也许<Object> {
        实例;

        @Override
        public boolean 有值() {
            return false;
        }

        @Override
        public Object 取() {
            throw new NoSuchElementException("无");
        }

        @Override
        public String toString() {
            return "无";
        }
    }

    enum 实例 implements 单子<也许.型, 实例.证>, 选择性<也许.型, 实例.证>, 笛卡尔<也许.型, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<也许.型, 内容> 纯(内容 值) {
            return 也许.有(值);
        }

        @Override
        public <内容, 新内容> 盒子<也许.型, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<也许.型, 新内容>> 功能,
                盒子<也许.型, 内容> 值) {
            也许<内容> 可能值 = 也许.拆(值);
            return 可能值.有值() ? 功能.用(可能值.取()) : 也许.无();
        }

        @Override
        public <内容, 新内容> 盒子<也许.型, 新内容> 选择(
                盒子<也许.型, 要么<内容, 新内容>> 值,
                盒子<也许.型, ? extends 功能<内容, 新内容>> 函数) {
            也许<要么<内容, 新内容>> 可能分支 = 也许.拆(值);
            if (可能分支.无值()) return 也许.无();
            要么<内容, 新内容> 分支 = 可能分支.取();
            if (分支.是右()) return 也许.有(分支.取右());
            return 也许.拆(函数).映射(函数值 -> 函数值.用(分支.取左()));
        }

        @Override
        public <内容, 新内容> 盒子<也许.型, 二元组<内容, 新内容>> 积(
                盒子<也许.型, 内容> 左,
                盒子<也许.型, 新内容> 右) {
            也许<内容> 左值 = 也许.拆(左);
            也许<新内容> 右值 = 也许.拆(右);
            return 左值.有值() && 右值.有值() ? 也许.有(new 二元组<>(左值.取(), 右值.取())) : 也许.无();
        }
    }
}

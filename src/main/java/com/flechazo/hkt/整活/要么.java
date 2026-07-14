package com.flechazo.hkt.整活;

public sealed interface 要么<左类型, 右类型> extends 盒子<要么.右型<左类型>, 右类型> permits 要么.左, 要么.右 {
    final class 右型<左类型> implements 一阶 {
        private 右型() {
        }
    }

    static <左参, 右参> 要么<左参, 右参> 左(左参 值) {
        return new 左<>(值);
    }

    static <左参, 右参> 要么<左参, 右参> 右(右参 值) {
        return new 右<>(值);
    }

    static <左参, 右参> 要么<左参, 右参> 拆(盒子<右型<左参>, 右参> 值) {
        return (要么<左参, 右参>) 值;
    }

    static <左参> 单子<右型<左参>, 实例.证> 单子() {
        return (单子<右型<左参>, 实例.证>) (单子<?, ?>) 实例.实例;
    }

    static <左参> 选择性<右型<左参>, 实例.证> 选择性() {
        return (选择性<右型<左参>, 实例.证>) (选择性<?, ?>) 实例.实例;
    }

    boolean 是左();

    default boolean 是右() {
        return !是左();
    }

    左类型 取左();

    右类型 取右();

    <新右> 要么<左类型, 新右> 映射(功能<? super 右类型, ? extends 新右> 功能);

    default <新右> 要么<左类型, 新右> 平绑(功能<? super 右类型, ? extends 要么<左类型, 新右>> 功能) {
        return 是右() ? 功能.用(取右()) : new 左<>(取左());
    }

    default <新左> 要么<新左, 右类型> 左映射(功能<? super 左类型, ? extends 新左> 功能) {
        return 是左() ? new 左<>(功能.用(取左())) : new 右<>(取右());
    }

    default <结果> 结果 折叠(功能<? super 左类型, ? extends 结果> 左功能,
                             功能<? super 右类型, ? extends 结果> 右功能) {
        return 是左() ? 左功能.用(取左()) : 右功能.用(取右());
    }

    default 要么<右类型, 左类型> 交换() {
        return 是左() ? new 右<>(取左()) : new 左<>(取右());
    }

    default 也许<右类型> 转也许() {
        return 是右() ? 也许.有(取右()) : 也许.无();
    }

    default 右类型 或默认(右类型 默认值) {
        return 是右() ? 取右() : 默认值;
    }

    record 左<左类型, 右类型>(左类型 错误) implements 要么<左类型, 右类型> {
        @Override
        public boolean 是左() {
            return true;
        }

        @Override
        public 左类型 取左() {
            return 错误;
        }

        @Override
        public 右类型 取右() {
            throw new IllegalStateException("左");
        }

        @Override
        public <新右> 要么<左类型, 新右> 映射(功能<? super 右类型, ? extends 新右> 功能) {
            return new 左<>(错误);
        }
    }

    record 右<左类型, 右类型>(右类型 值) implements 要么<左类型, 右类型> {
        @Override
        public boolean 是左() {
            return false;
        }

        @Override
        public 左类型 取左() {
            throw new IllegalStateException("右");
        }

        @Override
        public 右类型 取右() {
            return 值;
        }

        @Override
        public <新右> 要么<左类型, 新右> 映射(功能<? super 右类型, ? extends 新右> 功能) {
            return new 右<>(功能.用(值));
        }
    }

    enum 实例 implements 单子<右型<Object>, 实例.证>, 选择性<右型<Object>, 实例.证>, 笛卡尔<右型<Object>, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<右型<Object>, 内容> 纯(内容 值) {
            return 要么.右(值);
        }

        @Override
        public <内容, 新内容> 盒子<右型<Object>, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<右型<Object>, 新内容>> 功能,
                盒子<右型<Object>, 内容> 值) {
            要么<Object, 内容> 要么值 = 要么.拆(值);
            return 要么值.是左() ? 要么.左(要么值.取左()) : 功能.用(要么值.取右());
        }

        @Override
        public <内容, 新内容> 盒子<右型<Object>, 新内容> 选择(
                盒子<右型<Object>, 要么<内容, 新内容>> 值,
                盒子<右型<Object>, ? extends 功能<内容, 新内容>> 函数) {
            要么<Object, 要么<内容, 新内容>> 要么分支 = 要么.拆(值);
            if (要么分支.是左()) return 要么.左(要么分支.取左());
            要么<内容, 新内容> 分支 = 要么分支.取右();
            if (分支.是右()) return 要么.右(分支.取右());
            要么<Object, ? extends 功能<内容, 新内容>> 要么函数 = 要么.拆(函数);
            if (要么函数.是左()) return 要么.左(要么函数.取左());
            return 要么.右(要么函数.取右().用(分支.取左()));
        }

        @Override
        public <内容, 新内容> 盒子<右型<Object>, 二元组<内容, 新内容>> 积(
                盒子<右型<Object>, 内容> 左,
                盒子<右型<Object>, 新内容> 右) {
            要么<Object, 内容> 左值 = 要么.拆(左);
            要么<Object, 新内容> 右值 = 要么.拆(右);
            if (左值.是左()) return 要么.左(左值.取左());
            if (右值.是左()) return 要么.左(右值.取左());
            return 要么.右(new 二元组<>(左值.取右(), 右值.取右()));
        }
    }
}
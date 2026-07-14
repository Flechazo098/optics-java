package com.flechazo.hkt.整活;

public sealed interface 尝试<值类型> extends 盒子<尝试.型, 值类型> permits 尝试.成功, 尝试.失败 {
    final class 型 implements 一阶 {
        private 型() {
        }
    }

    static <值> 尝试<值> 成功(值 值) {
        return new 成功<>(值);
    }

    static <值> 尝试<值> 失败(Throwable 错误) {
        return new 失败<>(错误);
    }

    static <值> 尝试<值> 拆(盒子<尝试.型, 值> 值) {
        return (尝试<值>) 值;
    }

    static 实例 实例() {
        return 实例.实例;
    }

    static 单子<尝试.型, 实例.证> 单子() {
        return 实例.实例;
    }

    static 选择性<尝试.型, 实例.证> 选择性() {
        return 实例.实例;
    }

    boolean 是成功();

    default boolean 是失败() {
        return !是成功();
    }

    值类型 取();

    Throwable 取错误();

    <新值> 尝试<新值> 映射(功能<? super 值类型, ? extends 新值> 功能);

    default <新值> 尝试<新值> 平绑(功能<? super 值类型, ? extends 尝试<新值>> 功能) {
        if (是失败()) return 失败(取错误());
        try {
            return 功能.用(取());
        } catch (Exception e) {
            return 失败(e);
        }
    }

    default 尝试<值类型> 恢复(功能<? super Throwable, ? extends 值类型> 功能) {
        if (是成功()) return this;
        try {
            return 成功(功能.用(取错误()));
        } catch (Exception e) {
            return 失败(e);
        }
    }

    default 尝试<值类型> 恢复用(功能<? super Throwable, ? extends 尝试<值类型>> 功能) {
        if (是成功()) return this;
        try {
            return 功能.用(取错误());
        } catch (Exception e) {
            return 失败(e);
        }
    }

    default 也许<值类型> 转也许() {
        return 是成功() ? 也许.有(取()) : 也许.无();
    }

    record 成功<值类型>(值类型 值) implements 尝试<值类型> {
        @Override
        public boolean 是成功() {
            return true;
        }

        @Override
        public 值类型 取() {
            return 值;
        }

        @Override
        public Throwable 取错误() {
            throw new IllegalStateException("成功");
        }

        @Override
        public <新值> 尝试<新值> 映射(功能<? super 值类型, ? extends 新值> 功能) {
            try {
                return 成功(功能.用(值));
            } catch (Exception e) {
                return 失败(e);
            }
        }
    }

    record 失败<值类型>(Throwable 错误) implements 尝试<值类型> {
        @Override
        public boolean 是成功() {
            return false;
        }

        @Override
        public 值类型 取() {
            throw new IllegalStateException("失败");
        }

        @Override
        public Throwable 取错误() {
            return 错误;
        }

        @Override
        public <新值> 尝试<新值> 映射(功能<? super 值类型, ? extends 新值> 功能) {
            return 失败(错误);
        }
    }

    enum 实例 implements 单子<尝试.型, 实例.证>, 选择性<尝试.型, 实例.证>, 笛卡尔<尝试.型, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<尝试.型, 内容> 纯(内容 值) {
            return 尝试.成功(值);
        }

        @Override
        public <内容, 新内容> 盒子<尝试.型, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<尝试.型, 新内容>> 功能,
                盒子<尝试.型, 内容> 值) {
            return 尝试.拆(值).平绑(元素 -> 尝试.拆(功能.用(元素)));
        }

        @Override
        public <内容, 新内容> 盒子<尝试.型, 新内容> 选择(
                盒子<尝试.型, 要么<内容, 新内容>> 值,
                盒子<尝试.型, ? extends 功能<内容, 新内容>> 函数) {
            尝试<要么<内容, 新内容>> 尝试分支 = 尝试.拆(值);
            if (尝试分支.是失败()) return 尝试.失败(尝试分支.取错误());
            要么<内容, 新内容> 分支 = 尝试分支.取();
            if (分支.是右()) return 尝试.成功(分支.取右());
            尝试<? extends 功能<内容, 新内容>> 尝试函数 = 尝试.拆(函数);
            if (尝试函数.是失败()) return 尝试.失败(尝试函数.取错误());
            try {
                return 尝试.成功(尝试函数.取().用(分支.取左()));
            } catch (Exception e) {
                return 尝试.失败(e);
            }
        }

        @Override
        public <内容, 新内容> 盒子<尝试.型, 二元组<内容, 新内容>> 积(
                盒子<尝试.型, 内容> 左,
                盒子<尝试.型, 新内容> 右) {
            尝试<内容> 左值 = 尝试.拆(左);
            尝试<新内容> 右值 = 尝试.拆(右);
            if (左值.是失败()) return 尝试.失败(左值.取错误());
            if (右值.是失败()) return 尝试.失败(右值.取错误());
            return 尝试.成功(new 二元组<>(左值.取(), 右值.取()));
        }
    }
}
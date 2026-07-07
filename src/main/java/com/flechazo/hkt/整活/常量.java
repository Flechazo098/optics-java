package com.flechazo.hkt.整活;

public record 常量<日志类型, 值类型>(日志类型 日志) implements 盒子<常量.型<日志类型>, 值类型> {
    public static final class 型<日志类型> implements 一阶 {
        private 型() {
        }
    }

    public static <日志, 值> 常量<日志, 值> 仅日志(日志 日志值) {
        return new 常量<>(日志值);
    }

    public static <日志, 值> 常量<日志, 值> 拆(盒子<常量.型<日志>, 值> 值) {
        return (常量<日志, 值>) 值;
    }

    public static <日志> 函子<常量.型<日志>, 实例.证> 函子() {
        return new 实例<>();
    }

    public <新值> 常量<日志类型, 新值> 映射(功能<? super 值类型, ? extends 新值> 功能) {
        return new 常量<>(日志);
    }

    record 实例<日志>() implements 函子<常量.型<日志>, 实例.证> {
        static final class 证 implements 函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容, 新内容> 盒子<常量.型<日志>, 新内容> 映射(
                功能<? super 内容, ? extends 新内容> 功能,
                盒子<常量.型<日志>, 内容> 值) {
            return 常量.拆(值).映射(功能);
        }
    }
}
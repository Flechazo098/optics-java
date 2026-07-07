package com.flechazo.hkt.整活;

public record 写入器<日志, 值>(值 数据, 日志 记录) implements 盒子<写入器.型<日志>, 值> {
    public static final class 型<日志> implements 一阶 {
        private 型() {
        }
    }

    public static <日志, 值> 写入器<日志, 值> 写(值 数据, 日志 记录) {
        return new 写入器<>(数据, 记录);
    }

    public static <日志, 值> 写入器<日志, 值> 拆(盒子<写入器.型<日志>, 值> 值) {
        return (写入器<日志, 值>) 值;
    }

    public static <日志> 函子<写入器.型<日志>, 实例.证> 函子() {
        return new 实例<>();
    }

    public <新值> 写入器<日志, 新值> 映射(功能<? super 值, ? extends 新值> 功能) {
        return new 写入器<>(功能.用(数据), 记录);
    }

    record 实例<日志>() implements 函子<写入器.型<日志>, 实例.证> {
        static final class 证 implements 函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容, 新内容> 盒子<写入器.型<日志>, 新内容> 映射(
                功能<? super 内容, ? extends 新内容> 功能,
                盒子<写入器.型<日志>, 内容> 值) {
            return 写入器.拆(值).映射(功能);
        }
    }
}
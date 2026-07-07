package com.flechazo.hkt.整活;

public record 本体<值>(值 数据) implements 盒子<本体.型, 值> {
    public static final class 型 implements 一阶 {
        private 型() {
        }
    }

    public static <值> 本体<值> 纯(值 值) {
        return new 本体<>(值);
    }

    public static <值> 本体<值> 拆(盒子<本体.型, 值> 值) {
        return (本体<值>) 值;
    }

    public static 实例 实例() {
        return 实例.实例;
    }

    public static 单子<本体.型, 实例.证> 单子() {
        return 实例.实例;
    }

    public <新值> 本体<新值> 映射(功能<? super 值, ? extends 新值> 功能) {
        return new 本体<>(功能.用(数据));
    }

    enum 实例 implements 单子<本体.型, 实例.证>, 笛卡尔<本体.型, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<本体.型, 内容> 纯(内容 值) {
            return 本体.纯(值);
        }

        @Override
        public <内容, 新内容> 盒子<本体.型, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<本体.型, 新内容>> 功能,
                盒子<本体.型, 内容> 值) {
            return 功能.用(本体.拆(值).数据());
        }

        @Override
        public <内容, 新内容> 盒子<本体.型, 二元组<内容, 新内容>> 积(
                盒子<本体.型, 内容> 左,
                盒子<本体.型, 新内容> 右) {
            return 本体.纯(new 二元组<>(本体.拆(左).数据(), 本体.拆(右).数据()));
        }
    }
}
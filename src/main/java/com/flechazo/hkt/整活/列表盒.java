package com.flechazo.hkt.整活;

import java.util.ArrayList;
import java.util.List;

public record 列表盒<内容>(List<内容> 值) implements 盒子<列表盒.型, 内容> {
    public static final class 型 implements 一阶 {
        private 型() {
        }
    }

    public static <内容> 列表盒<内容> 装(List<? extends 内容> 值) {
        List<内容> 窄化 = (List<内容>) 值;
        return new 列表盒<>(窄化);
    }

    public static <内容> 列表盒<内容> 拆(盒子<列表盒.型, 内容> 值) {
        return (列表盒<内容>) 值;
    }

    public static 实例 实例() {
        return 实例.实例;
    }

    public static 应用函子<列表盒.型, 实例.证> 应用函子() {
        return 实例.实例;
    }

    public static 单子<列表盒.型, 实例.证> 单子() {
        return 实例.实例;
    }

    public static 选择性<列表盒.型, 实例.证> 选择性() {
        return 实例.实例;
    }

    public <新内容> 列表盒<新内容> 映射(功能<? super 内容, ? extends 新内容> 功能) {
        ArrayList<新内容> 结果 = new ArrayList<>(值.size());
        for (内容 内容值 : 值) {
            结果.add(功能.用(内容值));
        }
        return new 列表盒<>(结果);
    }

    enum 实例 implements 单子<列表盒.型, 实例.证>, 选择性<列表盒.型, 实例.证>, 笛卡尔<列表盒.型, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<列表盒.型, 内容> 纯(内容 值) {
            return 列表盒.装(List.of(值));
        }

        @Override
        public <内容, 新内容> 盒子<列表盒.型, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<列表盒.型, 新内容>> 功能,
                盒子<列表盒.型, 内容> 值) {
            ArrayList<新内容> 结果 = new ArrayList<>();
            for (内容 元素 : 列表盒.拆(值).值()) {
                结果.addAll(列表盒.拆(功能.用(元素)).值());
            }
            return 列表盒.装(结果);
        }

        @Override
        public <内容, 新内容> 盒子<列表盒.型, 新内容> 运用(
                盒子<列表盒.型, ? extends 功能<内容, 新内容>> 函数盒,
                盒子<列表盒.型, 内容> 值) {
            List<? extends 功能<内容, 新内容>> 函数列表 = 列表盒.拆(函数盒).值();
            List<内容> 值列表 = 列表盒.拆(值).值();
            ArrayList<新内容> 结果 = new ArrayList<>(函数列表.size() * 值列表.size());
            for (var 函数值 : 函数列表) {
                for (内容 元素 : 值列表) {
                    结果.add(函数值.用(元素));
                }
            }
            return 列表盒.装(结果);
        }

        @Override
        public <内容, 新内容> 盒子<列表盒.型, 新内容> 选择(
                盒子<列表盒.型, 要么<内容, 新内容>> 值,
                盒子<列表盒.型, ? extends 功能<内容, 新内容>> 函数) {
            List<要么<内容, 新内容>> 分支列表 = 列表盒.拆(值).值();
            List<? extends 功能<内容, 新内容>> 函数列表 = 列表盒.拆(函数).值();
            ArrayList<新内容> 结果 = new ArrayList<>();
            for (var 分支 : 分支列表) {
                if (分支.是右()) {
                    结果.add(分支.取右());
                } else {
                    for (var 函数值 : 函数列表) {
                        结果.add(函数值.用(分支.取左()));
                    }
                }
            }
            return 列表盒.装(结果);
        }

        @Override
        public <内容, 新内容> 盒子<列表盒.型, 二元组<内容, 新内容>> 积(
                盒子<列表盒.型, 内容> 左,
                盒子<列表盒.型, 新内容> 右) {
            ArrayList<二元组<内容, 新内容>> 结果 = new ArrayList<>();
            for (内容 a : 列表盒.拆(左).值()) {
                for (新内容 b : 列表盒.拆(右).值()) {
                    结果.add(new 二元组<>(a, b));
                }
            }
            return 列表盒.装(结果);
        }
    }
}
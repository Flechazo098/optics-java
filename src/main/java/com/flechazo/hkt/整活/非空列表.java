package com.flechazo.hkt.整活;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class 非空列表<值类型> implements 盒子<非空列表.型, 值类型>, Iterable<值类型> {
    public static final class 型 implements 一阶 {
        private 型() {
        }
    }

    private final 值类型 头;
    private final List<值类型> 尾;

    private 非空列表(值类型 头, List<值类型> 尾) {
        this.头 = 头;
        this.尾 = 尾;
    }

    public static <值> 非空列表<值> 单(值 头) {
        return new 非空列表<>(头, List.of());
    }

    public static <值> 非空列表<值> 装(值 头, List<? extends 值> 尾) {
        return new 非空列表<>(头, 窄化(尾));
    }

    public static <值> 也许<非空列表<值>> 从列表盒(列表盒<值> 盒子) {
        if (盒子.值().isEmpty()) return 也许.无();
        return 也许.有(new 非空列表<>(盒子.值().getFirst(), 盒子.值().subList(1, 盒子.值().size())));
    }

    public static <值> 非空列表<值> 拆(盒子<非空列表.型, 值> 值) {
        return (非空列表<值>) 值;
    }

    public static 实例 实例() {
        return 实例.实例;
    }

    public static 单子<非空列表.型, 实例.证> 单子() {
        return 实例.实例;
    }

    public 值类型 取头() {
        return 头;
    }

    public List<值类型> 取尾() {
        return 尾;
    }

    public int 大小() {
        return 尾.size() + 1;
    }

    public 列表盒<值类型> 转列表盒() {
        ArrayList<值类型> 所有 = new ArrayList<>(尾.size() + 1);
        所有.add(头);
        所有.addAll(尾);
        return 列表盒.装(所有);
    }

    public 非空列表<值类型> 追加(值类型 元素) {
        ArrayList<值类型> 新尾 = new ArrayList<>(尾.size() + 1);
        新尾.addAll(尾);
        新尾.add(元素);
        return new 非空列表<>(头, 新尾);
    }

    public 非空列表<值类型> 合并(非空列表<? extends 值类型> 其他) {
        ArrayList<值类型> 新尾 = new ArrayList<>(尾.size() + 其他.大小());
        新尾.addAll(尾);
        新尾.add(其他.头);
        新尾.addAll(其他.尾);
        return new 非空列表<>(头, 新尾);
    }

    public <新值> 非空列表<新值> 映射(功能<? super 值类型, ? extends 新值> 功能) {
        新值 新头 = 功能.用(头);
        ArrayList<新值> 新尾 = new ArrayList<>(尾.size());
        for (值类型 元素 : 尾) 新尾.add(功能.用(元素));
        return new 非空列表<>(新头, 新尾);
    }

    @Override
    public Iterator<值类型> iterator() {
        return new Iterator<>() {
            private boolean 在头 = true;
            private final Iterator<值类型> 尾迭代器 = 尾.iterator();

            @Override
            public boolean hasNext() {
                return 在头 || 尾迭代器.hasNext();
            }

            @Override
            public 值类型 next() {
                if (在头) {
                    在头 = false;
                    return 头;
                }
                return 尾迭代器.next();
            }
        };
    }

    @Override
    public String toString() {
        return 转列表盒().值().toString();
    }

    @SuppressWarnings("unchecked")
    private static <值> List<值> 窄化(List<? extends 值> 值列表) {
        return (List<值>) 值列表;
    }

    enum 实例 implements 单子<非空列表.型, 实例.证>, 笛卡尔<非空列表.型, 实例.证> {
        实例;

        static final class 证 implements 应用函子.型 {
            private 证() {
            }
        }

        @Override
        public <内容> 盒子<非空列表.型, 内容> 纯(内容 值) {
            return 非空列表.单(值);
        }

        @Override
        public <内容, 新内容> 盒子<非空列表.型, 新内容> 平绑(
                功能<? super 内容, ? extends 盒子<非空列表.型, 新内容>> 功能,
                盒子<非空列表.型, 内容> 值) {
            ArrayList<新内容> 结果 = new ArrayList<>();
            for (内容 o : 非空列表.拆(值)) {
                for (新内容 x : 非空列表.拆(功能.用(o))) 结果.add(x);
            }
            return 非空列表.装(结果.getFirst(), 结果.subList(1, 结果.size()));
        }

        @Override
        public <内容, 新内容> 盒子<非空列表.型, 新内容> 运用(
                盒子<非空列表.型, ? extends 功能<内容, 新内容>> 函数盒,
                盒子<非空列表.型, 内容> 值) {
            ArrayList<新内容> 结果 = new ArrayList<>();
            for (内容 元素 : 非空列表.拆(值)) {
                for (var 函数值 : 非空列表.拆(函数盒)) {
                    结果.add(函数值.用(元素));
                }
            }
            return 非空列表.装(结果.getFirst(), 结果.subList(1, 结果.size()));
        }

        @Override
        public <内容, 新内容> 盒子<非空列表.型, 二元组<内容, 新内容>> 积(
                盒子<非空列表.型, 内容> 左,
                盒子<非空列表.型, 新内容> 右) {
            ArrayList<二元组<内容, 新内容>> 结果 = new ArrayList<>();
            for (内容 a : 非空列表.拆(左)) {
                for (新内容 b : 非空列表.拆(右)) {
                    结果.add(new 二元组<>(a, b));
                }
            }
            return 非空列表.装(结果.getFirst(), 结果.subList(1, 结果.size()));
        }
    }
}
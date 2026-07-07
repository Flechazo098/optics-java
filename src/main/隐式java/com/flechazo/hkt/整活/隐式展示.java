package com.flechazo.hkt.整活;

import java.util.List;

public final class 隐式展示 {

    给定 也许函子<内容> {
        对于 也许<内容>;
        提供 函子<也许.型, 也许.实例.证>;
        使用 也许.实例();
        拆为 也许.拆;
    }

    给定 列表盒函子<内容> {
        对于 列表盒<内容>;
        提供 函子<列表盒.型, 列表盒.实例.证>;
        使用 列表盒.实例();
        拆为 列表盒.拆;
    }

    给定 本体函子<内容> {
        对于 本体<内容>;
        提供 函子<本体.型, 本体.实例.证>;
        使用 本体.实例();
        拆为 本体.拆;
    }

    public static <某盒子 extends 一阶, 证明 extends 函子.型, 内容> 盒子<某盒子, String> 显示(
            盒子<某盒子, 内容> 值
    ) 使用 (
            函子<某盒子, 证明> 函子
    ) {
        return 函子.映射(元素 -> "隐式拿到: " + 元素, 值);
    }

    public static void main(String[] 参数) {
        也许<String> 也许结果 = 隐式展示.显示(也许.有("苹果"));
        列表盒<String> 列表结果 = 隐式展示.显示(列表盒.装(List.of("苹果", "梨", "桃")));
        本体<String> 本体结果 = 隐式展示.显示(本体.纯(42));

        System.out.println("也许    -> " + 也许结果);
        System.out.println("列表盒  -> " + 列表结果.值());
        System.out.println("本体    -> " + 本体结果.数据());
    }
}

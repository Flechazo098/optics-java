package com.flechazo.hkt.整活;

import java.util.List;
import java.util.function.Function;

public final class 演示 {
    private 演示() {
    }

    public static <某盒子 extends 一阶, 证明 extends 函子.型, 内容> 盒子<某盒子, String> 描述(
            函子<某盒子, 证明> 函子,
            盒子<某盒子, 内容> 值) {
        return 函子.映射(元素 -> "value=" + 元素, 值);
    }

    static <输入, 输出> 功能<输入, 输出> 转功能(Function<输入, 输出> 函数) {
        return 函数::apply;
    }

    public static void main(String[] 参数) {
        System.out.println("=== 也许（Maybe） ===");
        也许<String> 也许结果 = 也许.拆(描述(也许.实例(), 也许.有("alex")));
        System.out.println("描述(有('alex')) = " + 也许结果);

        也许<String> 空结果 = 也许.拆(描述(也许.实例(), 也许.<String>无()));
        System.out.println("描述(无) = " + 空结果);

        System.out.println();
        System.out.println("=== 列表盒（ListK） ===");
        列表盒<String> 列表结果 = 列表盒.拆(描述(列表盒.实例(), 列表盒.装(List.of("alex", "sam", "bob"))));
        System.out.println("描述(列表) = " + 列表结果.值());

        System.out.println();
        System.out.println("=== 本体（Identity） ===");
        本体<Integer> id = 本体.纯(42);
        本体<String> 本体映射 = id.映射(转功能(n -> "数字=" + n));
        System.out.println("本体映射: " + 本体映射.数据());

        本体<String> 本体描述 = 本体.拆(描述(本体.实例(), 本体.纯("hello")));
        System.out.println("描述(本体('hello')) = " + 本体描述.数据());

        System.out.println();
        System.out.println("=== 要么（Either） ===");
        要么<String, Integer> 右值 = 要么.右(42);
        要么<String, String> 映射右 = 右值.映射(转功能((Integer n) -> "数字: " + n));
        System.out.println("右值折叠: " + 映射右.折叠(
                转功能(左 -> "左: " + 左),
                转功能(右 -> "右: " + 右)
        ));

        要么<String, Integer> 左值 = 要么.左("出错了");
        要么<String, String> 映射左 = 左值.映射(转功能(n -> "数字: " + n));
        System.out.println("左值折叠: " + 映射左.折叠(
                转功能(左 -> "左: " + 左),
                转功能(右 -> "右: " + 右)
        ));

        要么<Integer, Integer> 映射左错误 = 左值.左映射(转功能(String::length));
        System.out.println("左映射后: " + 映射左错误.折叠(
                转功能(左 -> "长度: " + 左),
                转功能(右 -> "值: " + 右)
        ));

        要么<Integer, String> 交换后 = 左值.交换();
        System.out.println("交换后: " + 交换后.折叠(
                转功能(左 -> "左(原右): " + 左),
                转功能(右 -> "右(原左): " + 右)
        ));

        System.out.println();
        System.out.println("=== 尝试（Try） ===");
        尝试<Integer> 成功 = 尝试.成功(100);
        尝试<String> 映射成功 = 成功.映射(转功能(n -> "结果: " + n));
        System.out.println("成功映射: " + (映射成功.是成功() ? 映射成功.取() : "失败"));

        尝试<Integer> 失败 = 尝试.失败(new RuntimeException("计算异常"));
        尝试<String> 映射失败 = 失败.映射(转功能(n -> "结果: " + n));
        System.out.println("失败映射: " + (映射失败.是成功() ? 映射失败.取()
                : "错误: " + 映射失败.取错误().getMessage()));

        尝试<Integer> 恢复 = 失败.恢复(转功能(e -> {
            System.out.println("  恢复: " + e.getMessage());
            return -1;
        }));
        System.out.println("恢复后: " + 恢复.取());
        System.out.println("转也许(成功) = " + 成功.转也许());
        System.out.println("转也许(失败) = " + 失败.转也许());

        System.out.println();
        System.out.println("=== 常量（Const） ===");
        常量<String, Integer> 日志常量 = 常量.仅日志("操作日志");
        常量<String, String> 映射常量 = 日志常量.映射(转功能(n -> "绝不执行"));
        System.out.println("常量日志(映射后不变): " + 映射常量.日志());

        System.out.println();
        System.out.println("=== 非空列表（NonEmptyList） ===");
        非空列表<String> 英文名 = 非空列表.装("Alice", List.of("Bob", "Charlie"));
        非空列表<String> 大写英文名 = 英文名.映射(转功能(String::toUpperCase));
        System.out.print("非空列表映射后: ");
        for (String 名 : 大写英文名) {
            System.out.print(名 + " ");
        }
        System.out.println();
        System.out.println("追加后大小: " + 英文名.追加("David").大小());
        System.out.println("合并后转列表盒: " + 英文名.合并(非空列表.单("Eve")).转列表盒().值());

        System.out.println();
        System.out.println("=== 类型间互转 ===");
        System.out.println("要么右转也许: " + 要么.<String, Integer>右(99).转也许());
        System.out.println("要么左转也许: " + 要么.<String, Integer>左("err").转也许());
        System.out.println("尝试成功转也许: " + 尝试.成功("OK").转也许());
        列表盒<String> 转列表盒 = 非空列表.装("x", List.of("y", "z")).转列表盒();
        System.out.println("非空列表转列表盒: " + 转列表盒.值());
        也许<非空列表<String>> 可能非空 = 非空列表.从列表盒(转列表盒);
        System.out.println("列表盒转回非空列表: " + (可能非空.有值() ? 可能非空.取().大小() : "空"));
        也许<非空列表<String>> 空情况 = 非空列表.从列表盒(列表盒.装(List.of()));
        System.out.println("空列表盒转非空: " + (空情况.有值() ? "有值" : "无"));

        System.out.println();
        System.out.println("=== 读取器（Reader） ===");
        读取器<String, Integer> 读长度 = String::length;
        System.out.println("读取器('hello') = " + 读长度.运行("hello"));
        读取器<String, String> 映射读取器 = 读长度.映射(转功能(n -> "长度=" + n));
        System.out.println("映射读取器('world') = " + 映射读取器.运行("world"));
        System.out.println("纯读取器('任意') = " + 读取器.<String, String>纯("你好").运行("任意"));
        读取器<String, Integer> 平绑读取器 = 读长度.平绑(n -> 读取器.<String, Integer>纯(n + 100));
        System.out.println("平绑读取器('test') = " + 平绑读取器.运行("test"));
        读取器<String, String> 询问演示 = 读取器.询问();
        System.out.println("询问('环境值') = " + 询问演示.运行("环境值"));
        读取器<Integer, String> 局部读取器 = 读取器.<String, String>纯("fixed").局部(转功能(n -> "#" + n));
        System.out.println("局部读取器(42) = " + 局部读取器.运行(42));

        System.out.println();
        System.out.println("=== 写入器（Writer） ===");
        写入器<String, Integer> 写结果 = 写入器.写(42, "初始化");
        写入器<String, String> 映射写 = 写结果.映射(转功能(n -> "值=" + n));
        System.out.println("写入器数据: " + 映射写.数据());
        System.out.println("写入器日志: " + 映射写.记录());

        System.out.println();
        System.out.println("=== 状态（State） ===");
        状态<Integer, String> 状态计算 = 状态.<Integer>获取().平绑(当前 ->
            状态.<Integer, String>纯("当前值: " + 当前));
        状态.状态结果<Integer, String> 状态计算结果 = 状态计算.运行(100);
        System.out.println("状态值: " + 状态计算结果.值());
        System.out.println("新状态: " + 状态计算结果.新状态());
        System.out.println("设置后状态: " + 状态.<Integer>设置(999).运行(0).新状态());
        System.out.println("修改后状态: " + 状态.<Integer>修改(转功能(n -> n * 2)).运行(21).新状态());
        状态<String, String> 组合状态 = 状态.<String>获取().平绑(当前 ->
            状态.<String, String>纯("Hello, " + 当前));
        状态.状态结果<String, String> 组合结果 = 组合状态.运行("World");
        System.out.println("组合状态值: " + 组合结果.值());
        System.out.println("组合状态新: " + 组合结果.新状态());

        System.out.println();
        System.out.println("=== 续延（Cont） ===");
        续延<String, Integer> 续延计算 = 续延.纯(42);
        续延<String, String> 映射续延 = 续延计算.映射(转功能(n -> "数字=" + n));
        System.out.println("续延运行: " + 映射续延.运行(转功能(s -> "结果: " + s)));
        续延<String, Integer> 平绑续延 = 续延.<String, Integer>纯(10).平绑(n ->
            续延.<String, Integer>纯(n * 3));
        System.out.println("平绑续延: " + 平绑续延.运行(转功能(n -> "最终: " + n)));

        System.out.println();
        System.out.println("=== HKT 函子（照英文版传实例） ===");
        System.out.println("描述(也许) = " + 也许.拆(描述(也许.实例(), 也许.有("hi"))));
        System.out.println("描述(列表盒) = " + 列表盒.拆(描述(列表盒.实例(), 列表盒.装(List.of("a", "b")))).值());
        System.out.println("描述(本体) = " + 本体.拆(描述(本体.实例(), 本体.纯("desc"))).数据());

        System.out.println();
        System.out.println("=== HKT 应用函子（纯 + 运用） ===");
        var 也许应用 = 也许.应用函子();
        System.out.println("也许纯(42) = " + 也许.<Integer>拆(也许应用.纯(42)).取());
        也许<功能<Integer, String>> 也许函数 = 也许.有(转功能(n -> "数=" + n));
        System.out.println("也许运用 = " + 也许.<String>拆(也许应用.运用(也许函数, 也许.有(99))).取());

        var 列表应用 = 列表盒.应用函子();
        System.out.println("列表纯(7) = " + 列表盒.<Integer>拆(列表应用.纯(7)).值());
        var 列表函数 = 列表盒.装(List.of(转功能((Integer n) -> n * 2), 转功能((Integer n) -> n * 10)));
        System.out.println("列表运用 = " + 列表盒.<Integer>拆(列表应用.运用(列表函数, 列表盒.装(List.of(3, 5)))).值());

        System.out.println();
        System.out.println("=== HKT 单子（平绑） ===");
        var 也许单子 = 也许.单子();
        也许<Integer> 也许平绑 = 也许.拆(也许单子.平绑((Integer n) -> 也许.有(n * 10), 也许.有(7)));
        System.out.println("也许平绑 = " + 也许平绑.取());
        var 列表单子 = 列表盒.单子();
        列表盒<Integer> 列表平绑 = 列表盒.拆(列表单子.平绑(
                (Integer n) -> 列表盒.装(List.of(n, n * 2)), 列表盒.装(List.of(1, 2))));
        System.out.println("列表平绑 = " + 列表平绑.值());

        System.out.println();
        System.out.println("=== HKT 选择 ===");
        var 也许选择 = 也许.选择性();
        也许<Integer> 选择右 = 也许.拆(也许选择.选择(也许.有(要么.<Integer, Integer>右(100)), 也许.无()));
        System.out.println("选择右 = " + 选择右.取());
        也许<Integer> 选择左 = 也许.拆(也许选择.选择(也许.有(要么.<Integer, Integer>左(5)), 也许.有(转功能((Integer x) -> x * 3))));
        System.out.println("选择左 = " + 选择左.取());
        var 列表选择 = 列表盒.选择性();
        var 选择结果 = 列表盒.<Integer>拆(列表选择.选择(
            列表盒.装(List.of(要么.<Integer, Integer>右(1), 要么.<Integer, Integer>左(3), 要么.<Integer, Integer>右(5))),
            列表盒.装(List.of(转功能((Integer x) -> x * 10)))));
        System.out.println("列表选择 = " + 选择结果.值());

        System.out.println();
        System.out.println("=== HKT 笛卡尔（积） ===");
        笛卡尔<也许.型, 也许.实例.证> 也许笛卡尔 = 也许.实例();
        也许<二元组<String, Integer>> 积结果 = 也许.拆(也许笛卡尔.积(也许.有("a"), 也许.有(1)));
        System.out.println("也许积 = (" + 积结果.取().左() + ", " + 积结果.取().右() + ")");
        笛卡尔<列表盒.型, 列表盒.实例.证> 列表笛卡尔 = 列表盒.实例();
        列表盒<二元组<String, Integer>> 列表积 = 列表盒.拆(列表笛卡尔.积(
            列表盒.装(List.of("x", "y")), 列表盒.装(List.of(1, 2, 3))));
        System.out.println("列表积 = " + 列表积.值());
    }
}

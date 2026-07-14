# Optics 使用与优化规范

本文规定 `optics-java` 的值语义前提、optic laws、容器重建边界、自动结构提升范围和推荐写法。

## 1. 适用范围

本项目面向已经采用函数式值语义的 Java 代码，不负责把任意可变对象转换为持久化数据结构，也不通过运行时包装替调用方管理 mutation。

### 1.1 lookup SPI 注册

在 JPMS 命名模块中使用 record lens、record traversal 和 sealed subtype 自动生成时，业务模块必须提供自己的 lookup：

```java
package com.example.internal;

import com.flechazo.optics.spi.OpticsLookupProvider;
import java.lang.invoke.MethodHandles;

public final class ApplicationOpticsLookup implements OpticsLookupProvider {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @Override
    public MethodHandles.Lookup lookup() {
        return LOOKUP;
    }
}
```

`OpticsLookupProvider` 支持 Java `ServiceLoader` 的两种标准注册方式。使用 JPMS 命名模块时，在业务模块的 `module-info.java` 中注册：

```java
module com.example.application {
    requires com.flechazo.optics;

    provides com.flechazo.optics.spi.OpticsLookupProvider
        with com.example.internal.ApplicationOpticsLookup;
}
```

使用 classpath 或未命名模块时，也可以创建：

```text
src/main/resources/META-INF/services/com.flechazo.optics.spi.OpticsLookupProvider
```

文件内容是 provider 的全限定类名：

```text
com.example.internal.ApplicationOpticsLookup
```

这两种方式提供的是同一项 SPI 服务，按部署方式选择其一即可：命名模块使用 `provides ... with ...`，classpath 使用 `META-INF/services`。普通单 classpath 部署中，业务类与 optics 库通常已经处于同一个未命名模块，此时自动生成不要求额外注册；显式的 service 文件仍适用于分离类加载器等需要由业务侧提供 lookup 的部署。

provider 所在包不需要 `exports` 或 `opens`。每个需要生成自身私有类型 optic 的命名模块应提供一个 provider；注册完成后，业务代码继续直接使用 `Lens.of`、`Traversal.of`、`Prism.subtype` 和 `Optics.*`。lookup 必须由 provider 类自身调用 `MethodHandles.lookup()` 获得，不能返回 `publicLookup()`，也不能返回其他模块的 lookup。

本文中的“不可变”首先指语义不可变：

```text
更新不修改旧值
更新产生新逻辑值
旧值及其可达结构在之后保持不变
```

它不要求每个运行时对象的 mutation 方法都抛出 `UnsupportedOperationException`。一个新建的 `ArrayList` 或 `LinkedHashMap`，在库和调用方都不再修改它时，可以作为不可变值参与计算。

## 2. 调用方必须遵守的不可变性契约

### 2.1 Source 不得被修改

传给以下操作的 source 必须被视为不可变值：

```java
optic.modify(function, source);
optic.set(value, source);
optic.modifyF(function, source, applicative);
fold.foldMap(monoid, mapper, source);
fold.preview(source);
fold.getAll(source);
```

调用结束后也不得通过已有别名修改 source 或 source 可达的嵌套对象。

不允许：

```java
Lens<User, Address> address = Lens.of(
        User::address,
        (user, value) -> {
            user.mutableAddressField = value;
            return user;
        });
```

推荐：

```java
Lens<User, Address> address = Lens.of(
        User::address,
        (user, value) -> new User(user.name(), value));
```

### 2.2 嵌套集合也属于值的一部分

record 本身不可重新赋值，不代表其字段自动不可变：

```java
record Team(List<User> users) {}
```

`users` 进入 optic 运算后，不得在外部调用 `add`、`remove`、`clear` 或原地修改其中元素。更新应创建新的逻辑集合值和新的外层值。

### 2.3 Getter、preview 和 targets 必须是纯观察

以下函数不能修改 source，也不能依赖会在同一次计算中变化的隐藏状态：

- lens getter；
- iso getter；
- prism matcher；
- affine preview；
- traversal targets；
- getter/fold reader。

不得用 getter 做计数、缓存写入、I/O 或容器 mutation。如果需要显式 effect，应使用 `modifyF` 和相应 Applicative 表达，而不是把 effect 藏进普通 getter。

### 2.4 Rebuilder 必须产生新逻辑值

setter/rebuilder 可以在函数内部使用未逃逸的局部 mutable builder，但不得修改 source，也不得让多个结果共享之后还会被修改的 accumulator。

允许：

```java
(source, replacements) -> {
    ArrayList<Item> result = new ArrayList<>(replacements);
    return new Basket(result);
}
```

前提是 `result` 返回后继续按不可变值使用。

不允许：

```java
(source, replacements) -> {
    source.items().clear();
    source.items().addAll(replacements);
    return source;
}
```

### 2.5 Modifier 的要求

普通 `modify` 的 modifier 应是确定性的纯函数：

```java
city.modify(String::trim, user);
```

需要日志、验证、状态、失败或多分支语义时，使用 `modifyF` 和对应的 `Applicative`。库会保持 Applicative 的 effect 组合顺序，并使用持久化 accumulator 隔离分支。

### 2.6 返回集合的使用规则

标准容器 traversal 返回新建的 JDK 容器，但不承诺：

- 保留 source 的具体实现类；
- 返回 Guava immutable collection；
- mutation 方法一定抛异常；
- 保留任意自定义 `Map`/`Set` 的 comparator、identity equality 或并发语义。

调用方必须继续按不可变值使用返回结果。

对于 `List<A>`、`Set<A>`、`Map<K,V>`，契约关注：

- 元素、key 和 value；
- 声明的遍历/effect 顺序；
- set 去重；
- map key 保留；
- source 未被修改。

具体类和运行时 mutation 防护不属于抽象值语义。

需要保留第三方持久化集合或特殊容器语义时，应使用显式 targets/rebuild：

```java
Traversal<CustomBag<Item>, Item> items = Traversal.of(
        CustomBag::values,
        (bag, replacements) -> CustomBag.from(replacements));
```

如果该模式无法被证明为已知结构，它会安全保留为 opaque optic；正确性不受影响，只是 optimizer 不穿透该节点。

## 3. Optic laws

调用方提供的 optic 应满足相应 laws。优化器以这些 laws 和已证明结构为前提。

### 3.1 Lens

对于合法 lens：

```text
get(set(a, s)) = a
set(get(s), s) = s
set(b, set(a, s)) = set(b, s)
```

这里的等号是项目定义的值相等，不要求对象 identity 或具体集合类相同。

### 3.2 Iso

```text
reverseGet(get(s)) = s
get(reverseGet(a)) = a
```

### 3.3 Prism/Affine

匹配成功后的 build/rebuild 必须与 preview 的 focus 对应；匹配失败必须保留原有 miss 语义，不能偷偷更新其他分支。

### 3.4 Traversal

Traversal 必须保持 focus 顺序和 Applicative effect 顺序。rebuilder 必须用与 targets 对应的 replacement 序列重建逻辑值。

标准 set traversal 允许 modifier 将多个元素映射成同一个值，结果遵循 set 去重语义。map-entry traversal 修改 key 后若发生碰撞，结果遵循 map key 唯一性语义。

## 4. 自动执行管线

所有核心 runtime optic 都携带未公开的 `OpticProgram<S,T,A,B>`。用户调用 `andThen` 时只形成 program composition：

```text
left program + right program
  -> OpticProgram.Compose
```

组合阶段不会执行：

- lowering；
- optimizer；
- backend；
- hidden-class generation。

终端调用时才自动执行：

```text
terminal operation + complete program shape
  -> lowering
  -> PointFreeOptimizer.optimize
  -> class-file backend
  -> hidden-class executor
  -> shape cache
```

普通用户不应直接调用 internal terminal runtime。

## 5. 终端缓存边界

缓存键包含：

- 终端操作类型；
- opaque/structured/compose 节点类别；
- 完整 compose 左右 spine；
- structured kind；
- record path、subtype、container key 等结构化 key。

以下运行时值不进入缓存键：

- `modify` modifier；
- `set` value；
- source；
- `foldMap` monoid；
- `foldMap` mapper；
- query predicate。

这些值通过每次调用的 runtime slot 传入。相同 shape 可以复用 executor，不会捕获第一次调用的 modifier、value 或 source。

## 6. 能自动优化的 Lens 写法

结构分析依赖 optic 工厂的可序列化函数接口。直接写在工厂参数位置的 lambda/方法引用会选择 `LensGetter`、`LensRebuilder` 等专用重载：

```java
Lens<User, Address> address = Lens.of(
        User::address,
        (user, value) -> new User(user.name(), value));
```

如果先把同一个函数擦除成普通 JDK `Function`/`BiFunction`，再传给工厂，就会选择不分析的兼容重载：

```java
Function<User, Address> getter = User::address;
BiFunction<User, Address, User> rebuild =
        (user, value) -> new User(user.name(), value);

Lens<User, Address> address = Lens.of(getter, rebuild);
```

后一种写法仍然正确，但会保留为 opaque。需要先保存函数再构造 optic 时，应保留专用静态类型：

```java
LensGetter<User, Address> getter = User::address;
LensRebuilder<User, Address, User> rebuild =
        (user, value) -> new User(user.name(), value);

Lens<User, Address> address = Lens.of(getter, rebuild);
```

### 6.1 Record 单字段

推荐：

```java
Lens<User, Address> address = Lens.of(
        user -> user.address(),
        (user, value) -> new User(user.name(), value));
```

方法引用同样适用：

```java
Lens<User, Address> address = Lens.of(
        User::address,
        (user, value) -> new User(user.name(), value));
```

也可以使用 record 工厂：

```java
Lens<User, Address> address = Lens.of(User.class, User::address);
```

### 6.2 嵌套 Record path

```java
Lens<User, String> city = Lens.of(
        user -> user.address().city(),
        (user, value) -> new User(
                user.name(),
                new Address(value, user.address().zip())));
```

读取路径和重建路径必须一致。上例可以提升为结构化 record path。

### 6.3 不会提升的错误配对

```java
Lens<User, String> invalid = Lens.of(
        user -> user.address().city(),
        (user, value) -> new User(value, user.address()));
```

getter 读取 city，但 setter 把 replacement 写入 name。该 pair 无法证明为 lens path，因此保持 opaque。

### 6.4 显式 opaque

```java
Lens<User, Address> address = Lens.opaque(
        User::address,
        (user, value) -> new User(user.name(), value));
```

`opaque` 工厂不会尝试结构分析。

## 7. 能自动优化的 Iso 写法

单字段 record accessor 与构造器可以提升：

```java
record Box(String value) {}

Iso<Box, String> boxed = Iso.of(Box::value, Box::new);
```

任意复杂转换仍可作为普通 iso 使用；无法证明时为 opaque。

## 8. 能自动优化的 Prism 写法

### 8.1 Subtype

优先使用明确工厂：

```java
Prism<Shape, Circle> circle = Prism.subtype(
        Shape.class,
        Circle.class);
```

可分析的条件式也能提升：

```java
Prism<Shape, Integer> radius = Prism.of(
        shape -> shape instanceof Circle circle
                ? Either.right(circle.radius())
                : Either.left(shape),
        Circle::new);
```

### 8.2 已知 sum type

当前能够识别 canonical 分支和 builder，包括：

- `Maybe`；
- `Optional`；
- `Either`；
- `Validated`；
- `Try`。

示例：

```java
Prism<Maybe<String>, String> some = Prism.of(
        maybe -> maybe.isDefined()
                ? Either.right(maybe.get())
                : Either.left(Maybe.none()),
        Maybe::some);
```

matcher 必须保留 miss 分支，builder 必须重建同一个 focus 分支。

## 9. 能自动优化的 Affine 写法

### 9.1 已知容器

```java
Affine<Map<String, User>, User> selected =
        Affine.mapValue("selected");

Affine<List<User>, User> second =
        Affine.listAt(1);
```

### 9.2 Canonical helper pair

```java
String key = "selected";

Affine<Map<String, User>, User> selected = Affine.of(
        source -> AffinePreview.mapValue(source, key),
        (source, value) -> AffineRebuilder.mapValue(source, key, value));
```

preview 和 rebuild 必须使用同一个 key/index。读取 `a`、写入 `b` 的 pair 会保持 opaque。

### 9.3 Maybe/Optional

canonical present/defined conditional 与同分支 rebuild 可以提升为专用 affine 节点。

## 10. 能自动优化的 Traversal 写法

### 10.1 标准容器工厂

优先使用：

```java
Traversal<List<A>, A> list = Traversals.forList();
Traversal<Set<A>, A> set = Traversals.forSet();
Traversal<Map<K, V>, V> values = Traversals.forMapValues();
Traversal<Map<K, V>, Tuple2<K, V>> entries = Traversals.forMapEntries();
Traversal<A[], A> array = Traversals.forArray(A.class);
Traversal<String, Character> characters = StringTraversals.characters();
```

这些工厂直接产生已知 structured program。

### 10.2 Record container

```java
record Team(List<User> users) {}

Traversal<Team, User> users = Traversal.of(
        Team.class,
        Team::users);
```

该 convenience 工厂要求 getter 是可分析的单个 record component，并且 component 是可 traversal 的容器。

完整 targets/rebuild 写法：

```java
Traversal<Team, User> users = Traversal.of(
        Team::users,
        (team, replacements) -> new Team(replacements));
```

### 10.3 Canonical container helper

以下成对 helper 可以提升为专用节点：

```java
Traversal.of(WanderGetter::list, WanderRebuilder::list);
Traversal.of(WanderGetter::set, WanderRebuilder::set);
Traversal.of(WanderGetter::mapValues, WanderRebuilder::mapValues);
Traversal.of(WanderGetter::mapEntries, WanderRebuilder::mapEntries);
Traversal.of(
        WanderGetter::stringCharacters,
        WanderRebuilder::stringCharacters);
Traversal.ofArray(
        String.class,
        WanderGetter::array,
        WanderRebuilder::array);
```

array 必须通过 `ofArray(componentType, ...)` 提供运行时 component type。

### 10.4 任意自定义 traversal

```java
Traversal<Team, User> users = Traversal.of(
        team -> computeUsers(team),
        (team, replacements) -> new Team(replacements));
```

如果 `computeUsers` 无法被分析，factory 不会抛异常，而是产生 opaque traversal。它仍能正确执行、组合并进入统一 terminal runtime。

需要保证永远不分析时：

```java
Traversal<Team, User> users = Traversal.opaque(
        Team::users,
        (team, replacements) -> new Team(replacements));
```

## 11. Getter、Fold 和 Setter

canonical record getter/fold/setter 可以提升：

```java
Getter<User, Address> address = Getter.of(User::address);

Fold<Team, User> users = Fold.of(Team::users);

Setter<User, Address> addressSetter = Setter.of(
        (modifier, user) -> new User(
                user.name(),
                modifier.apply(user.address())));
```

如果 reader 经由任意 helper 方法间接调用，或者 setter 把 modifier 结果写入不相关字段，则保持 opaque。

## 12. Composition

推荐把小而可证明的 optic 组合起来：

```java
Lens<User, String> city =
        userAddress.andThen(addressCity);

Traversal<Team, String> cities =
        teamUsers
                .andThen(userAddress)
                .andThen(addressCity);
```

所有核心 simple optic、`P*` optic、query optic 和 indexed optic 的组合都会保留 program spine。

精确组合会尽量保留能力最强的返回类型。例如：

```text
Lens >> Iso       -> Lens
Lens >> Prism     -> Affine
Lens >> Traversal -> Traversal
PIso >> PLens     -> PLens
PTraversal >> PIso -> PTraversal
```

组合中只要存在 opaque 节点，整体仍可正确执行，但 optimizer 不会穿透该 opaque 边界。

## 13. 终端 modifier 不需要可分析

结构提升分析的是 optic 的结构定义，例如 getter 与 rebuilder；不是每次调用的业务 modifier。

以下 modifier 都可以使用同一个已编译 executor：

```java
city.modify(String::trim, user);
city.modify(String::toUpperCase, user);
city.modify(value -> "[" + value + "]", user);
```

modifier 通过 runtime slot 传入，不进入 program shape，也不会被固化进缓存 class。

同理，下面两个 `set` value 不会生成两个 executor：

```java
city.set("Paris", user);
city.set("Tokyo", user);
```

## 14. 容易退回 opaque 的写法

以下情况通常无法结构化提升：

- getter、matcher 或 targets 调用任意无法内联证明的 helper；
- getter 与 setter/rebuilder 指向不同字段、key 或 index；
- setter 读取或写入隐藏 mutable state；
- 通过反射、动态代理或任意 virtual dispatch 决定路径；
- 非 canonical 条件分支；
- builder 没有重建 matcher 所识别的同一 sum 分支；
- 自定义容器没有已知 canonical helper pair；
- 显式使用 `opaque` 工厂。

opaque 不是错误，也不会退出编译管线。它表示：

```text
正确执行 direct semantics
允许 terminal shape cache
允许 generic hidden-class terminal delegate
禁止 optimizer 穿透、复制、重排该节点
```

## 15. 推荐实践

1. 优先使用 record、sealed hierarchy 和项目提供的 persistent/value types。
2. 把 optic 定义成小的、可复用的结构，再用 `andThen` 组合。
3. getter 使用直接 accessor 或简单 accessor chain。
4. setter/rebuilder 使用 canonical constructor rebuild。
5. 标准容器优先使用 `Traversal.forList/forSet/mapValues/...`。
6. map key/list index 优先使用 `Affine.mapValue/listAt`。
7. subtype 优先使用 `Prism.subtype`。
8. effect 使用 `modifyF` 表达，不要隐藏在 getter/setter 中。
9. 不依赖返回容器具体实现，也不修改返回容器。
10. 无法证明或有意建立优化边界时使用 opaque，不要为了提升而伪造结构。

## 16. 验收一段 optic 是否可优化

从 public API 角度，正确做法不是读取 internal program，而是验证：

- direct 语义满足 optic laws；
- source 未被修改；
- 不同 modifier/value/source 可以安全复用；
- `modify` 与相应 `modifyF` 在值语义上等价；
- 组合前后 effect 顺序一致；
- opaque fallback 仍然正确。

项目内部测试会进一步检查 structured kind、program spine、specialized terminal count、hidden class 和 cache reuse。这些 internal 检查不应成为业务代码依赖。

# optics-java

一个简化实现的 Java 光学库，同时包含一套轻量 HKT 编码和若干常用函数式数据类型。

本项目的部分设计、命名和 API 组织参考了：

- [higher-kinded-j](https://github.com/higher-kinded-j/higher-kinded-j) [MIT]
- [DataFixerUpper](https://github.com/Mojang/DataFixerUpper) [MIT]

这不是上述项目的兼容实现，也不追求逐项复刻。这里只是用于一个配置库的基础实现。

## 核心概念

### HKT 编码

部分参考了 [DataFixerUpper](https://github.com/Mojang/DataFixerUpper) 的设计。

HKT 使用 `K1`/`K2` 作为 kind marker，使用 `App`/`App2` 表示应用：

```java
public interface K1 {}

public interface K2 {}

public interface App<F extends K1, A> {}

public interface App2<F extends K2, A, B> extends App<App2.Mu<F, A>, B> {}
```

一元类型构造子使用自己的 `Mu` 作为 witness，例如：

- `Maybe.Mu`
- `Try.Mu`
- `IdF.Mu`

二元类型构造子使用 `K2` witness，并通过 `App2.Mu<F, A>` 固定第一个类型参数，例如：

- `Either.Mu`
- `Validated.Mu`
- `Tuple2.Mu`

### Typeclass

当前包含：

- `Functor`
- `Applicative`
- `Monad`
- `Selective`
- `Natural`
- `Semigroup`
- `Monoid`

`Selective` 有两个方法，它们有不同的语义：

- `select(...)` 是 selective 核心操作，两个结构参数已经给出。
- `ifS(...)` 是 lazy conditional，只请求被选中的分支。

这意味着 `Validated.select(...)` 可以累积已经提供的结构中的错误，而 `Validated.ifS(...)` 会像条件分支一样只执行被选分支。

其他没什么值得讲的地方。

### 数据类型

当前包含：

- `Maybe<A>`
- `Either<L, R>`
- `Try<A>`
- `Validated<E, A>`
- `Tuple2<A, B>`
- `IdF<A>`
- `Unit`


- 普通 value position 可以是 `null`，例如 `Maybe.some(null)` 和 `Try.success(null)`。
- 结构性控制值不能为 `null`，例如 `Selective.select(...)` 中成功得到的 `Either` 必须非 null。
- 结构性函数值不能为 `null`，例如 applicative function 和 selective function 必须非 null。
- `Semigroup`/`Monoid` 的代数值不支持 `null`。
- `Tuple2` 本身允许 nullable component；但当它通过 `Tuple2.monad(...)`、`Tuple2.applicative(...)` 或 `Tuple2.selective(...)` 被解释为 writer-style instance 时，第一个 component 是 writer log，必须是非 null monoid value。
- `Optional` 只出现在 util adapter 层。核心内部使用 `Maybe`，仅是为了符合 Java 编码习惯。

其他没什么值得讲的地方。

## Optics
此模块的 API 命名设计完全参考了 [higher-kinded-j](https://github.com/higher-kinded-j/higher-kinded-j)

提供的方法不算多，仅提供较为基础的方法，多了也用不上。

核心：

- `Iso<S, A>`
- `Lens<S, A>`
- `Prism<S, A>`
- `Affine<S, A>`
- `Traversal<S, A>`
- `Fold<S, A>`
- `Getter<S, A>`
- `Setter<S, A>`
- `Optic<S, T, A, B>`

Indexed optics：

- `IndexedOptic<I, S, A>`
- `IndexedLens<I, S, A>`
- `IndexedTraversal<I, S, A>`
- `IndexedFold<I, S, A>`
- `Pair<A, B>`

实例和工具：

- `EachInstances`
- `IxedInstances`
- `AtInstances`
- `Traversals`
- `Prisms`
- `Affines`
- `Optionals`
- `ListPrisms`
- `ListTraversals`
- `StringTraversals`
- `EitherTraversals`
- `TryTraversals`
- `ValidatedTraversals`
- `TupleTraversals`

## Class-file 生成

本项目通过 class-file 生成和缓存来构造高性能实现。

主要入口：

- `RecordOptics.recordLens(recordType, "component")`
- `RecordOptics.recordLens(recordType, RecordType::component)`
- `RecordOptics.recordTraversal(recordType, "component")`
- `RecordOptics.subtypePrism(sealedType, subtype)`
- `ClassFileOptics.lens(recordType, RecordType::component)`
- `ClassFileOptics.lenses(recordType)`
- `ClassFileOptics.traversals(recordType)`
- `SpecOptics.generate(specInterface, recordType)`

## Focus DSL

`com.flechazo.optics.focus` 提供轻量 DSL，用于把生成出的 focus path 继续组合到 lens / traversal / affine 等 optic。

## 依赖

主要依赖：

- `io.smallrye.classfile:jdk-classfile-backport`

本项目基于 Java 21 构建，因此需要 Class File API 移植版。
# optics-java

`optics-java` 是一个独立的 Java 光学、HKT、类型代数和优化器实验库。当前重点是把 [DataFixerUpper](https://github.com/Mojang/DataFixerUpper)（以下简称 DFU）里和类型重写、无点式优化、递归类型处理等相关的理论部分，用更适合 [OELib](https://github.com/Tower-of-Sighs/OELib) 的方式重新实现。

## DFU 理论重写

本项目基于 DataFixerUpper 的 HKT、光学、无点式重写、类型重写和递归类型重写相关部分做了本地重写。这里并没有完全复刻 DFU，而是抽取其中的理论内核，用作配置库底层。

本项目已经解决了若干 DFU 已知问题：

- 积类型、和类型、标签选择分支排序后的类型修复 [DFU / Issue#108](https://github.com/Mojang/DataFixerUpper/issues/108)。
- 递归重写中的分支/索引裁剪。
- 递归 cata/fold 融合的显式依赖证据。
- ReflexCata 的相等性判断问题，改为显式恒等代数证据。
- 字段/类型查找统一路径。
- 递归点的模板贯穿和 μ 边界修复。

## 新增理论实现

在 DFU 已有思想之外，本项目主要补上了这些理论实现：

- Java 侧结构化类型代数，把记录、标签选择、密封子类型、递归族槽位等项目需要的类型结构纳入优化器可检查的数据。
- 四参数带类型光学脊柱：`S -> T`、`A -> B` 分离，优化器可以在重写后修复光学元数据。
- fold/query IR，用于 fold-map 融合、积式 fold 融合、preview、first、any、all、count 等查询优化。
- 通用递归特化，把结构化通用递归函数降到 `CataPlan`。
- 证据驱动的 ReflexCata，只有显式自反恒等证据和依赖证据足够时才消成恒等函数。

## 优化器

优化器是一个分层构造的等式重写系统，只在结构信息和类型证据足够时改写表达式，本质是 IR + 一个小型优化编译器。

底层是带类型的无点式 IR，将光学操作拆解为组合子树（组合、应用、恒等、bang、Fn、opticApp、cataPlan 等），使表达式可代数操作。中层是策略式重写引擎，用 topDown、bottomUp、once、many、all、one 等组合子将遍历策略与变换规则解耦。上层是四参数类型化光学脊柱，将光学表示为可检查、可比较、可前缀提取/后缀切分的带类型元素序列——每条光学是一条 profunctor 变换 P A B → P S T。

另外，为了给不同写法也应用这一套优化逻辑，本项目实现了一套小型编译器前端。
它读取可序列化 lambda 的 class-file 字节码并构建 `LambdaExpr` AST，在结构可证明时提升为专用 optic 节点，无法证明时则保留 opaque 语义。此篇讲了什么写法会被优化，什么不会 [optics-usage-contract](docs/optics-usage-contract.md)。

应用通过 `OpticsLookupProvider` SPI 一次性提供自身的 `MethodHandles.Lookup`，既可在 JPMS 的 `module-info.java` 中使用 `provides` 注册，也可在 classpath 下使用 `META-INF/services` 注册；之后仍直接使用 `Lens.of`、`Traversal.of`、`Prism.subtype` 和 `Optics.*`，无需在每次调用时传 lookup。配置方式见 [optics-usage-contract](docs/optics-usage-contract.md#11-lookup-spi-注册)。

## 性能结论

在真实应用型迁移基准中，本项目与 DFU 基本持平。把优化器场景单独拆出来看，两边仍会出现差异，整体属于平分秋色：有 DFU 更高的地方，也有本项目更高的地方。

本项目在优化器里保留了更严格的理论检查，因此部分单独拆出的优化器场景可能因为这些检查而不如 DFU，但真实开发中这类差异影响微乎其微。

## 主要模块

### HKT 与类型类

基础 HKT 编码使用 `K1`、`K2`、`App`、`App2`、`Kind1`、`Kind2` 表示一元和二元类型构造子应用。当前包含的类型类和能力接口主要有：

- `Functor`
- `Applicative`
- `Selective`
- `Monad`
- `Contravariant`
- `Bifunctor`
- `Natural`
- `Semigroup`
- `Monoid`
- `Foldable`
- `Traversable`
- `Profunctor`
- `Cartesian`
- `Strong`
- `Cocartesian`
- `Choice`
- `Closed`
- `Traversing`
- `Mapping`
- `AffineP`
- `Monoidal`
- `MonoidProfunctor`
- `FunctorProfunctor`
- `ReCartesian`
- `ReCocartesian`

核心 profunctor carrier 和辅助结构包括：

- `FunctionArrow`
- `Forget`
- `ForgetE`
- `ForgetOpt`
- `ReForget`
- `ReForgetC`
- `ReForgetE`
- `ReForgetP`
- `ReForgetEP`
- `Procompose`
- `Const`
- `Grate`
- `Wander`

核心数据类型包括：

- `Maybe<A>`
- `Either<L, R>`
- `Try<A>`
- `Validated<E, A>`
- `Tuple2<A, B>`
- `Tuple3<A, B, C>`
- `Tuple4<...>` 至 `Tuple16<...>`
- `IdF<A>`
- `Unit`

### 业务 API

业务能力接口包括：

- `Composable<A>`
- `Combinable<A>`
- `Chainable<A>`
- `Effectful<A>`
- `Recoverable<E, A>`
- `Accumulating<E, A>`

业务组合入口和路径类型包括：

- `Pathway`
- `MaybePath<A>`
- `EitherPath<E, A>`
- `TryPath<A>`
- `ValidationPath<E, A>`
- `ListPath<A>`
- `ReaderPath<R, A>`
- `WriterPath<W, A>`
- `WithStatePath<S, A>`
- `LazyPath<A>`
- `VTaskPath<A>`
- `IOPath<A>`
- `ResourcePath<A>`
- `IOResourcePath<A>`
- `CompletableFuturePath<A>`
- `StreamPath<A>`
- `VStreamPath<A>`

业务上下文、控制和 effect 数据类型包括：

- `Reader<R, A>`
- `Writer<W, A>`
- `State<S, A>`
- `StateResult<S, A>`
- `ListK<A>`
- `ValidatedNel`
- `NonEmptyList<A>`
- `Lazy<A>`
- `VTask<A>`
- `IO<A>`
- `Resource<A>`
- `IOResource<A>`

业务 resilience API 包括：

- `Resilience`
- `ResilienceBuilder<A>`
- `Retry`
- `RetryPolicy`
- `RetryEvent`
- `CircuitBreaker`
- `CircuitBreakerConfig`
- `CircuitBreakerMetrics`
- `Bulkhead`
- `BulkheadConfig`
- `Saga<A>`
- `SagaBuilder<A>`
- `SagaStep<A>`
- `SagaError`

业务 stream API 包括：

- `StreamK<A>`
- `VStream<A>`
- `VStreamPar`
- `VStreamReactive`
- `VStreamThrottle`
- `VStreamFunctor`
- `VStreamApplicative`
- `VStreamMonad`
- `VStreamAlternative`
- `VStreamTraverse`

业务辅助 API 包括：

- `Attempts`
- `Monoids`
- `Semigroups`
- `Traverses`
- `OptionalOps`

### 光学

核心光学 API 包括：

- `Iso<S, A>`
- `Lens<S, A>`
- `Prism<S, A>`
- `Affine<S, A>`
- `Traversal<S, A>`
- `Fold<S, A>`
- `Getter<S, A>`
- `Setter<S, A>`
- `Optic<S, T, A, B>`

多态光学 API 包括：

- `PIso<S, T, A, B>`
- `PLens<S, T, A, B>`
- `PPrism<S, T, A, B>`
- `PAffine<S, T, A, B>`
- `PTraversal<S, T, A, B>`
- `PSetter<S, T, A, B>`

带索引光学包括：

- `IndexedOptic<I, S, A>`
- `IndexedLens<I, S, A>`
- `IndexedTraversal<I, S, A>`
- `IndexedGetter<I, S, A>`
- `IndexedFold<I, S, A>`

光学语义函数接口包括：

- `LensGetter<S, A>`
- `LensRebuilder<S, A, T>`
- `IsoGetter<S, A>`
- `IsoRebuilder<A, S>`
- `PrismMatcher<S, T, A>`
- `PrismBuilder<A, S>`
- `AffinePreview<S, T, A>`
- `AffineRebuilder<S, A, T>`
- `GetterReader<S, A>`
- `FoldGetter<S, A>`
- `WanderGetter<S, A>`
- `WanderRebuilder<S, A>`
- `SetterModifier<S, T, A, B>`

常用实例和工具包括：

- `Optics`
- `Each`
- `Ixed`
- `At`
- `Traversals`
- `Prisms`
- `Affines`
- `ListPrisms`
- `ListTraversals`
- `StringTraversals`
- `EitherTraversals`
- `TryTraversals`
- `ValidatedTraversals`
- `TupleTraversals`

### Focus DSL

`com.flechazo.optics.focus` 提供轻量焦点 DSL，用于把生成出的焦点路径继续组合到 lens、traversal、affine 等光学结构。

参考：

Optimizing functions:
- [Cunha, A., & Pinto, J. S. (2005). Point-free program transformation](https://scholar.google.com/scholar?q=Cunha%2C%20A.%2C%20%26%20Pinto%2C%20J.%20S.%20%282005%29.%20Point-free%20program%20transformation)
- [Lämmel, R., Visser, E., & Visser, J. (2002). The essence of strategic programming](https://scholar.google.com/scholar?q=L%C3%A4mmel%2C%20R.%2C%20Visser%2C%20E.%2C%20%26%20Visser%2C%20J.%20%282002%29.%20The%20essence%20of%20strategic%20programming)

How to handle recursive types:
- [Cunha, A., & Pacheco, H. (2011). Algebraic specialization of generic functions for recursive types](https://scholar.google.com/scholar?q=Cunha%2C%20A.%2C%20%26%20Pacheco%2C%20H.%20%282011%29.%20Algebraic%20specialization%20of%20generic%20functions%20for%20recursive%20types)
- [Yakushev, A. R., Holdermans, S., Löh, A., & Jeuring, J. (2009, August). Generic programming with fixed points for mutually recursive datatypes](https://scholar.google.com/scholar?q=Yakushev%2C%20A.%20R.%2C%20Holdermans%2C%20S.%2C%20L%C3%B6h%2C%20A.%2C%20%26%20Jeuring%2C%20J.%20%282009%2C%20August%29.%20Generic%20programming%20with%20fixed%20points%20for%20mutually%20recursive%20datatypes)
- [Magalhães, J. P., & Löh, A. (2012). A formal comparison of approaches to datatype-generic programming](https://scholar.google.com/scholar?q=Magalh%C3%A3es%2C%20J.%20P.%2C%20%26%20L%C3%B6h%2C%20A.%20%282012%29.%20A%20formal%20comparison%20of%20approaches%20to%20datatype-generic%20programming)

Optics:
- [Pickering, M., Gibbons, J., & Wu, N. (2017). Profunctor Optics: Modular Data Accessors](https://scholar.google.com/scholar?q=Pickering%2C%20M.%2C%20Gibbons%2C%20J.%2C%20%26%20Wu%2C%20N.%20%282017%29.%20Profunctor%20Optics%3A%20Modular%20Data%20Accessors)
- [Pacheco, H., & Cunha, A. (2010, June). Generic point-free lenses](https://scholar.google.com/scholar?q=Pacheco%2C%20H.%2C%20%26%20Cunha%2C%20A.%20%282010%2C%20June%29.%20Generic%20point-free%20lenses)

Tying it together:
- [Cunha, A., Oliveira, J. N., & Visser, J. (2006, August). Type-safe two-level data transformation](https://scholar.google.com/scholar?q=Cunha%2C%20A.%2C%20Oliveira%2C%20J.%20N.%2C%20%26%20Visser%2C%20J.%20%282006%2C%20August%29.%20Type-safe%20two-level%20data%20transformation)
- [Cunha, A., & Visser, J. (2011). Transformation of structure-shy programs with application to XPath queries and strategic functions](https://scholar.google.com/scholar?q=Cunha%2C%20A.%2C%20%26%20Visser%2C%20J.%20%282011%29.%20Transformation%20of%20structure-shy%20programs%20with%20application%20to%20XPath%20queries%20and%20strategic%20functions)
- [Pacheco, H., & Cunha, A. (2011, January). Calculating with lenses: optimising bidirectional transformations](https://scholar.google.com/scholar?q=Pacheco%2C%20H.%2C%20%26%20Cunha%2C%20A.%20%282011%2C%20January%29.%20Calculating%20with%20lenses%3A%20optimising%20bidirectional%20transformations)

### class-file 字节码后端

本项目使用 `Class File API` 生成隐藏类执行器。字节码后端会先运行标准优化器，再为支持的无点式计划生成专用执行器/函数类；不支持的节点保留解释执行兜底路径。

## 构建要求

本项目基于 Java 21 构建。

主要依赖：

- `io.smallrye.classfile:jdk-classfile-backport`

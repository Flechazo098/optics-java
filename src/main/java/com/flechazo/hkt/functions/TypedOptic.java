package com.flechazo.hkt.functions;

import com.flechazo.hkt.*;
import com.flechazo.hkt.type.TaggedChoice;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record TypedOptic<S, T, A, B>(
        Set<TypeToken<? extends K1>> bounds,
        List<? extends Element<?, ?, ?, ?>> elements) {
    public interface Optic<Proof extends K1, S, T, A, B> {
        <P extends K2> FunctionArrow<App2<P, A, B>, App2<P, S, T>> eval(App<Proof, P> proof);
    }

    public TypedOptic {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(elements, "elements");
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("typed optic spine must not be empty");
        }
    }

    public TypedOptic(
            TypeToken<? extends K1> bound,
            Type<S> sType,
            Type<T> tType,
            Type<A> aType,
            Type<B> bType,
            PointFreeOpticElement optic) {
        this(Set.of(bound), List.of(new Element<>(sType, tType, aType, bType, optic)));
    }

    public Type<S> sType() {
        return outermostElement().sType();
    }

    public Type<T> tType() {
        return outermostElement().tType();
    }

    public Type<A> aType() {
        return innermostElement().aType();
    }

    public Type<B> bType() {
        return innermostElement().bType();
    }

    public PointFreeOpticElement outermost() {
        return outermostElement().optic();
    }

    public PointFreeOpticElement innermost() {
        return innermostElement().optic();
    }

    public <P extends K2, Proof extends K1> App2<P, S, T> apply(
            TypeToken<Proof> token,
            App<Proof, P> proof,
            App2<P, A, B> argument) {
        Maybe<Optic<Proof, S, T, A, B>> optic = upCast(token);
        if (optic.isEmpty()) {
            throw new IllegalArgumentException("Couldn't upcast optic to proof " + token);
        }
        return optic.get()
                .eval(proof)
                .apply(argument);
    }

    public <Proof extends K1> Maybe<Optic<Proof, S, T, A, B>> upCast(TypeToken<Proof> proof) {
        Objects.requireNonNull(proof, "proof");
        if (!instanceOf(bounds, proof)) {
            return Maybe.none();
        }
        return Maybe.some(new Optic<>() {
            @Override
            public <P extends K2> FunctionArrow<App2<P, A, B>, App2<P, S, T>> eval(App<Proof, P> proofBox) {
                Objects.requireNonNull(proofBox, "proofBox");
                return FunctionArrow.of(argument -> evalSpine(proofBox, argument));
            }
        });
    }

    public static <Proof extends K1> boolean instanceOf(
            Collection<TypeToken<? extends K1>> bounds,
            TypeToken<Proof> proof) {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(proof, "proof");
        return bounds.stream().allMatch(bound -> bound.isSupertypeOf(proof));
    }

    public List<PointFreeOpticElement> elementOptics() {
        return elements.stream().map(Element::optic).toList();
    }

    public T modify(Function<? super A, ? extends B> function, S source) {
        return cast(modifyFrom(0, castFunction(function), source));
    }

    public boolean isIdentity() {
        return elements.size() == 1 && outermost().kind() == PointFreeOpticKind.ADAPTER
                && Objects.equals(sType(), aType())
                && Objects.equals(tType(), bType());
    }

    public int size() {
        return elements.size();
    }

    public int commonPrefixLength(TypedOptic<?, ?, ?, ?> other) {
        int size = Math.min(elements.size(), other.elements.size());
        for (int i = 0; i < size; i++) {
            if (!elements.get(i).sameOptic(other.elements.get(i))) {
                return i;
            }
        }
        return size;
    }

    public boolean sameElements(TypedOptic<?, ?, ?, ?> other) {
        return elements.size() == other.elements.size()
                && commonPrefixLength(other) == elements.size();
    }

    public boolean containsOnly(PointFreeOpticKind kind) {
        return elements.stream().allMatch(element -> element.optic().kind() == kind);
    }

    public boolean startsWith(PointFreeOpticKind kind) {
        return outermost().kind() == kind;
    }

    public TypedOptic<S, T, Object, Object> prefix(int size) {
        if (size < 1 || size > elements.size()) {
            throw new IndexOutOfBoundsException(size);
        }
        return new TypedOptic<>(bounds, elements.subList(0, size));
    }

    public TypedOptic<Object, Object, A, B> suffix(int from) {
        if (from < 0 || from >= elements.size()) {
            throw new IndexOutOfBoundsException(from);
        }
        return new TypedOptic<>(bounds, elements.subList(from, elements.size()));
    }

    public <A1, B1> TypedOptic<S, T, A1, B1> compose(TypedOptic<A, B, A1, B1> other) {
        ImmutableSet.Builder<TypeToken<? extends K1>> nextBounds = ImmutableSet.builder();
        nextBounds.addAll(bounds);
        nextBounds.addAll(other.bounds);
        ImmutableList.Builder<Element<?, ?, ?, ?>> nextElements =
                ImmutableList.builderWithExpectedSize(elements.size() + other.elements.size());
        nextElements.addAll(elements);
        nextElements.addAll(other.elements);
        return new TypedOptic<>(nextBounds.build(), nextElements.build());
    }

    public <S2, T2> TypedOptic<S2, T2, A, B> castOuter(Type<S2> sType, Type<T2> tType) {
        ImmutableList.Builder<Element<?, ?, ?, ?>> nextElements =
                ImmutableList.builderWithExpectedSize(elements.size());
        nextElements.add(outermostElement().castOuter(sType, tType));
        nextElements.addAll(elements.subList(1, elements.size()));
        return new TypedOptic<>(bounds, nextElements.build());
    }

    @Override
    public String toString() {
        return "(" + elements.stream().map(Object::toString).collect(Collectors.joining(" \u25E6 ")) + ")";
    }

    @SuppressWarnings("unchecked")
    private Element<S, T, ?, ?> outermostElement() {
        return (Element<S, T, ?, ?>) elements.getFirst();
    }

    @SuppressWarnings("unchecked")
    private Element<?, ?, A, B> innermostElement() {
        return (Element<?, ?, A, B>) elements.getLast();
    }

    @SuppressWarnings("unchecked")
    private <P extends K2, Proof extends K1> App2<P, S, T> evalSpine(
            App<Proof, P> proof,
            App2<P, A, B> argument) {
        App2<P, Object, Object> current = (App2<P, Object, Object>) argument;
        for (int i = elements.size() - 1; i >= 0; i--) {
            current = evalElement(proof, elements.get(i), current);
        }
        return (App2<P, S, T>) current;
    }

    @SuppressWarnings("unchecked")
    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalElement(
            App<Proof, P> proof,
            Element<?, ?, ?, ?> element,
            App2<P, Object, Object> argument) {
        PointFreeOpticElement optic = element.optic();
        return switch (optic) {
            case AdapterOpticElement ignored -> Profunctor
                    .unbox(proofAs(proof))
                    .dimap(Function.identity(), Function.identity())
                    .apply(argument);
            case LensOpticElement lens -> evalLens(proof, lens, argument);
            case ProductOpticElement product -> evalProduct(proof, product, argument);
            case SumOpticElement sum -> evalSum(proof, sum, argument);
            case TaggedOpticElement tagged -> evalTagged(proof, tagged, argument);
            case PrismOpticElement<?, ?, ?, ?> prism ->
                    evalPrism(proof, (PrismOpticElement<Object, Object, Object, Object>) prism, argument);
            case AffineOpticElement<?, ?, ?, ?> affine ->
                    evalAffine(proof, (AffineOpticElement<Object, Object, Object, Object>) affine, argument);
            case SubtypeOpticElement subtype -> evalSubtype(proof, subtype, argument);
            case TraversalOpticElement traversal -> evalTraversal(proof, traversal, argument);
            case MapOpticElement map -> evalMap(proof, map, argument);
            case FoldOpticElement<?, ?> ignored ->
                    throw new UnsupportedOperationException("Fold optic elements are query-only and cannot be applied");
        };
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalLens(
            App<Proof, P> proof,
            LensOpticElement lens,
            App2<P, Object, Object> argument) {
        Cartesian<P, Cartesian.Mu> cartesian = Cartesian.unbox(proofAs(proof));
        App2<P, Pair<Object, Object>, Pair<Object, Object>> focused = cartesian.first(argument);
        return cartesian.dimap(
                focused,
                source -> Pair.of(lens.element().get(source), source),
                pair -> lens.element().set(pair.first(), pair.second()));
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalProduct(
            App<Proof, P> proof,
            ProductOpticElement product,
            App2<P, Object, Object> argument) {
        Cartesian<P, Cartesian.Mu> cartesian = Cartesian.unbox(proofAs(proof));
        return switch (product.side()) {
            case FIRST -> app2AsObject(cartesian.first(argument));
            case SECOND -> app2AsObject(cartesian.second(argument));
        };
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalSum(
            App<Proof, P> proof,
            SumOpticElement sum,
            App2<P, Object, Object> argument) {
        Cocartesian<P, Cocartesian.Mu> cocartesian = Cocartesian.unbox(proofAs(proof));
        return switch (sum.side()) {
            case LEFT -> app2AsObject(cocartesian.left(argument));
            case RIGHT -> app2AsObject(cocartesian.right(argument));
        };
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalTagged(
            App<Proof, P> proof,
            TaggedOpticElement tagged,
            App2<P, Object, Object> argument) {
        Cocartesian<P, Cocartesian.Mu> cocartesian = Cocartesian.unbox(proofAs(proof));
        App2<P, Either<Object, Object>, Either<Object, Object>> focused = cocartesian.left(argument);
        return cocartesian.dimap(
                focused,
                source -> {
                    Pair<?, ?> pair = (Pair<?, ?>) source;
                    return Objects.equals(tagged.tag(), pair.first()) ? Either.left(pair.second()) : Either.right(source);
                },
                result -> result.isLeft() ? Pair.of(tagged.tag(), result.left()) : result.right());
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalPrism(
            App<Proof, P> proof,
            PrismOpticElement<Object, Object, Object, Object> prism,
            App2<P, Object, Object> argument) {
        Cocartesian<P, Cocartesian.Mu> cocartesian = Cocartesian.unbox(proofAs(proof));
        App2<P, Either<Object, Object>, Either<Object, Object>> focused = cocartesian.left(argument);
        return cocartesian.dimap(
                focused,
                source -> prism.prism().match(source).fold(Either::right, Either::left),
                result -> result.isLeft() ? prism.prism().build(result.left()) : result.right());
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalAffine(
            App<Proof, P> proof,
            AffineOpticElement<Object, Object, Object, Object> affine,
            App2<P, Object, Object> argument) {
        AffineP<P, AffineP.Mu> affineP = AffineP.unbox(proofAs(proof));
        App2<P, Pair<Object, Object>, Pair<Object, Object>> paired = affineP.first(argument);
        App2<P, Either<Pair<Object, Object>, Object>, Either<Pair<Object, Object>, Object>> focused =
                affineP.left(paired);
        return affineP.dimap(
                focused,
                source -> affine.affine().preview(source)
                        .fold(Either::right, value -> Either.left(Pair.of(value, source))),
                result -> result.isLeft()
                        ? affine.affine().set(result.left().first(), result.left().second())
                        : result.right());
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalSubtype(
            App<Proof, P> proof,
            SubtypeOpticElement subtype,
            App2<P, Object, Object> argument) {
        AffineP<P, AffineP.Mu> affineP = AffineP.unbox(proofAs(proof));
        App2<P, Pair<Object, Object>, Pair<Object, Object>> paired = affineP.first(argument);
        App2<P, Either<Pair<Object, Object>, Object>, Either<Pair<Object, Object>, Object>> focused =
                affineP.left(paired);
        return affineP.dimap(
                focused,
                source -> subtype.subtype().isInstance(source)
                        ? Either.left(Pair.of(source, source))
                        : Either.right(source),
                result -> result.isLeft() ? result.left().first() : result.right());
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalTraversal(
            App<Proof, P> proof,
            TraversalOpticElement traversal,
            App2<P, Object, Object> argument) {
        Traversing<P, Traversing.Mu> traversing = Traversing.unbox(proofAs(proof));
        return traversing.wander(new Wander<>() {
            @Override
            public <F extends K1> FunctionArrow<Object, App<F, Object>> wander(
                    Applicative<F, ?> applicative,
                    FunctionArrow<Object, App<F, Object>> input) {
                return FunctionArrow.of(source -> {
                    if (traversal.traversal() != null) {
                        return traversal.traversal().modifyF(input, source, applicative);
                    }
                    if (Objects.equals(traversal.key(), "maybe")) {
                        Maybe<?> maybe = (Maybe<?>) source;
                        if (maybe.isEmpty()) {
                            return applicative.of(Maybe.none());
                        }
                        return applicative.map(Maybe::some, input.apply(maybe.get()));
                    }
                    if (Objects.equals(traversal.key(), "stringCharacters")) {
                        String string = (String) source;
                        App<F, StringBuilder> acc = applicative.of(new StringBuilder(string.length()));
                        for (int i = 0; i < string.length(); i++) {
                            acc = applicative.map2(acc, input.apply(string.charAt(i)), (builder, next) -> {
                                StringBuilder copy = new StringBuilder(builder);
                                copy.append(next);
                                return copy;
                            });
                        }
                        return applicative.map(StringBuilder::toString, acc);
                    }
                    if (Objects.equals(traversal.key(), "validatedValid")) {
                        Validated<?, ?> validated = (Validated<?, ?>) source;
                        if (validated.isInvalid()) {
                            return applicative.of(validated);
                        }
                        return applicative.map(Validated::valid, input.apply(validated.value()));
                    }
                    if (Objects.equals(traversal.key(), "validatedInvalid")) {
                        Validated<?, ?> validated = (Validated<?, ?>) source;
                        if (validated.isValid()) {
                            return applicative.of(validated);
                        }
                        return applicative.map(Validated::invalid, input.apply(validated.error()));
                    }
                    List<?> values = (List<?>) source;
                    App<F, ImmutableList.Builder<Object>> acc =
                            applicative.of(ImmutableList.builderWithExpectedSize(values.size()));
                    for (Object value : values) {
                        acc = applicative.map2(acc, input.apply(value), (builder, next) -> {
                            builder.add(next);
                            return builder;
                        });
                    }
                    return applicative.map(ImmutableList.Builder::build, acc);
                });
            }
        }, argument);
    }

    private static <P extends K2, Proof extends K1> App2<P, Object, Object> evalMap(
            App<Proof, P> proof,
            MapOpticElement map,
            App2<P, Object, Object> argument) {
        Traversing<P, Traversing.Mu> traversing = Traversing.unbox(proofAs(proof));
        return traversing.wander(new Wander<>() {
            @Override
            public <F extends K1> FunctionArrow<Object, App<F, Object>> wander(
                    Applicative<F, ?> applicative,
                    FunctionArrow<Object, App<F, Object>> input) {
                return FunctionArrow.of(source -> {
                    java.util.Map<?, ?> values = (java.util.Map<?, ?>) source;
                    App<F, ImmutableList.Builder<Pair<Object, Object>>> acc =
                            applicative.of(ImmutableList.builderWithExpectedSize(values.size()));
                    for (java.util.Map.Entry<?, ?> entry : values.entrySet()) {
                        Object key = entry.getKey();
                        App<F, Pair<Object, Object>> nextValue = switch (map.target()) {
                            case VALUES -> applicative.map(value -> Pair.of(key, value), input.apply(entry.getValue()));
                            case ENTRIES -> applicative.map(
                                    TypedOptic::castPair,
                                    input.apply(Pair.of(key, entry.getValue())));
                        };
                        acc = applicative.map2(acc, nextValue, (builder, next) -> {
                            builder.add(next);
                            return builder;
                        });
                    }
                    return applicative.map(builder -> {
                        Object2ObjectLinkedOpenHashMap<Object, Object> result =
                                new Object2ObjectLinkedOpenHashMap<>();
                        for (Pair<Object, Object> entry : builder.build()) {
                            result.put(entry.first(), entry.second());
                        }
                        return result;
                    }, acc);
                });
            }
        }, argument);
    }

    @SuppressWarnings("unchecked")
    private static <Proof extends K1, P extends K2, Bound extends K1> App<Bound, P> proofAs(App<Proof, P> proof) {
        return (App<Bound, P>) proof;
    }

    @SuppressWarnings("unchecked")
    private static <F extends K1, A> App<F, A> asApp(App<?, ?> value) {
        return (App<F, A>) value;
    }

    @SuppressWarnings("unchecked")
    private static <P extends K2> App2<P, Object, Object> app2AsObject(App2<P, ?, ?> value) {
        return (App2<P, Object, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Pair<Object, Object> castPair(Object value) {
        return (Pair<Object, Object>) value;
    }

    private Object modifyFrom(
            int index,
            Function<Object, Object> function,
            Object source) {
        if (index == elements.size()) {
            return function.apply(source);
        }
        Element<?, ?, ?, ?> element = elements.get(index);
        return element.optic().modify(value -> modifyFrom(index + 1, function, value), source);
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }

    @SuppressWarnings("unchecked")
    private static Function<Object, Object> castFunction(
            Function<?, ?> function) {
        return (Function<Object, Object>) function;
    }

    public static <S, T> TypedOptic<S, T, S, T> adapter(Type<S> sType, Type<T> tType) {
        return new TypedOptic<>(
                Profunctor.Mu.TYPE_TOKEN,
                sType,
                tType,
                sType,
                tType,
                new AdapterOpticElement());
    }

    public static <S, T, A, B> TypedOptic<S, T, A, B> retag(
            Type<S> sType,
            Type<T> tType,
            Type<A> aType,
            Type<B> bType) {
        return new TypedOptic<>(
                Profunctor.Mu.TYPE_TOKEN,
                sType,
                tType,
                aType,
                bType,
                new AdapterOpticElement());
    }

    public static <F, G, F2> TypedOptic<Pair<F, G>, Pair<F2, G>, F, F2> proj1(
            Type<F> fType,
            Type<G> gType,
            Type<F2> newType) {
        return new TypedOptic<>(
                Cartesian.Mu.TYPE_TOKEN,
                Types.and(fType, gType),
                Types.and(newType, gType),
                fType,
                newType,
                new ProductOpticElement(ProductSide.FIRST));
    }

    public static <F, G, G2> TypedOptic<Pair<F, G>, Pair<F, G2>, G, G2> proj2(
            Type<F> fType,
            Type<G> gType,
            Type<G2> newType) {
        return new TypedOptic<>(
                Cartesian.Mu.TYPE_TOKEN,
                Types.and(fType, gType),
                Types.and(fType, newType),
                gType,
                newType,
                new ProductOpticElement(ProductSide.SECOND));
    }

    public static <L, R, L2> TypedOptic<Either<L, R>, Either<L2, R>, L, L2> inj1(
            Type<L> lType,
            Type<R> rType,
            Type<L2> newType) {
        return new TypedOptic<>(
                Cocartesian.Mu.TYPE_TOKEN,
                Types.or(lType, rType),
                Types.or(newType, rType),
                lType,
                newType,
                new SumOpticElement(SumSide.LEFT));
    }

    public static <L, R, R2> TypedOptic<Either<L, R>, Either<L, R2>, R, R2> inj2(
            Type<L> lType,
            Type<R> rType,
            Type<R2> newType) {
        return new TypedOptic<>(
                Cocartesian.Mu.TYPE_TOKEN,
                Types.or(lType, rType),
                Types.or(lType, newType),
                rType,
                newType,
                new SumOpticElement(SumSide.RIGHT));
    }

    public static <A, B> TypedOptic<List<A>, List<B>, A, B> list(Type<A> aType, Type<B> bType) {
        return new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                Types.list(aType),
                Types.list(bType),
                aType,
                bType,
                TraversalOpticElement.list());
    }

    public static <A, B> TypedOptic<Maybe<A>, Maybe<B>, A, B> maybe(Type<A> aType, Type<B> bType) {
        return new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                Types.maybe(aType),
                Types.maybe(bType),
                aType,
                bType,
                TraversalOpticElement.maybe());
    }

    public static TypedOptic<String, String, Character, Character> stringCharacters() {
        return new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                Types.witness(String.class),
                Types.witness(String.class),
                Types.witness(Character.class),
                Types.witness(Character.class),
                TraversalOpticElement.stringCharacters());
    }

    public static <E, A, B> TypedOptic<Validated<E, A>, Validated<E, B>, A, B> validatedValid(
            Type<E> errorType,
            Type<A> valueType,
            Type<B> newValueType) {
        return new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                Types.validated(errorType, valueType),
                Types.validated(errorType, newValueType),
                valueType,
                newValueType,
                TraversalOpticElement.validatedValid());
    }

    public static <E, F, A> TypedOptic<Validated<E, A>, Validated<F, A>, E, F> validatedInvalid(
            Type<E> errorType,
            Type<F> newErrorType,
            Type<A> valueType) {
        return new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                Types.validated(errorType, valueType),
                Types.validated(newErrorType, valueType),
                errorType,
                newErrorType,
                TraversalOpticElement.validatedInvalid());
    }

    public static <K, A, B> TypedOptic<Pair<K, ?>, Pair<K, ?>, A, B> tagged(
            TaggedChoice.TaggedChoiceType<K> sType,
            K key,
            Type<A> aType,
            Type<B> bType) {
        return new TypedOptic<>(
                Cocartesian.Mu.TYPE_TOKEN,
                sType,
                replaceTagged(sType, key, aType, bType),
                aType,
                bType,
                new TaggedOpticElement(key));
    }

    private static <K, A, B> Type<Pair<K, ?>> replaceTagged(
            TaggedChoice.TaggedChoiceType<K> sType,
            K key,
            Type<A> aType,
            Type<B> bType) {
        if (Objects.equals(aType, bType)) {
            return sType;
        }
        if (!Objects.equals(sType.choice(key), aType)) {
            throw new IllegalArgumentException("Focused type does not match tagged choice branch");
        }
        Object2ObjectOpenHashMap<K, Type<?>> next =
                new Object2ObjectOpenHashMap<>(sType.types());
        next.put(key, bType);
        return Types.taggedChoiceType(sType.name(), sType.keyType(), next);
    }

    public record Element<S, T, A, B>(
            Type<S> sType,
            Type<T> tType,
            Type<A> aType,
            Type<B> bType,
            PointFreeOpticElement optic) {
        public Element {
            Objects.requireNonNull(sType, "sType");
            Objects.requireNonNull(tType, "tType");
            Objects.requireNonNull(aType, "aType");
            Objects.requireNonNull(bType, "bType");
            Objects.requireNonNull(optic, "optic");
        }

        public <S2, T2> Element<S2, T2, A, B> castOuter(Type<S2> sType, Type<T2> tType) {
            return new Element<>(sType, tType, aType, bType, optic);
        }

        boolean sameOptic(Element<?, ?, ?, ?> other) {
            return optic.sameOptic(other.optic);
        }

        @Override
        public String toString() {
            return optic.toString();
        }
    }
}

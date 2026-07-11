package com.flechazo.hkt.functions;

import com.flechazo.hkt.*;
import com.flechazo.hkt.type.TaggedChoice;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PAffine;
import com.flechazo.optics.Fold;
import com.flechazo.optics.PPrism;
import com.flechazo.optics.PTraversal;
import com.google.common.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public sealed interface PointFreeOptic<S, T, A, B> permits CompositePointFreeOptic {
    TypedOptic<S, T, A, B> typed();

    default List<PointFreeOpticElement> elements() {
        return typed().elementOptics();
    }

    default T modify(Function<? super A, ? extends B> function, S source) {
        return typed().modify(function, source);
    }

    default PointFreeOpticTypes<S, T, A, B> types() {
        TypedOptic<S, T, A, B> typed = typed();
        return new PointFreeOpticTypes<>(typed.sType(), typed.tType(), typed.aType(), typed.bType());
    }

    default Type<S> sourceType() {
        return typed().sType();
    }

    default Type<T> targetType() {
        return typed().tType();
    }

    default Type<A> focusType() {
        return typed().aType();
    }

    default Type<B> replacementType() {
        return typed().bType();
    }

    default Set<TypeToken<? extends K1>> bounds() {
        return typed().bounds();
    }

    default PointFreeOpticElement outermost() {
        return typed().outermost();
    }

    default boolean isIdentity() {
        return typed().isIdentity();
    }

    default int size() {
        return typed().size();
    }

    default int commonPrefixLength(PointFreeOptic<?, ?, ?, ?> other) {
        return typed().commonPrefixLength(other.typed());
    }

    default boolean sameElements(PointFreeOptic<?, ?, ?, ?> other) {
        return typed().sameElements(other.typed());
    }

    default boolean containsOnly(PointFreeOpticKind kind) {
        return typed().containsOnly(kind);
    }

    default boolean startsWith(PointFreeOpticKind kind) {
        return typed().startsWith(kind);
    }

    default PointFreeOptic<S, T, Object, Object> prefix(int size) {
        return new CompositePointFreeOptic<>(typed().prefix(size));
    }

    default PointFreeOptic<Object, Object, A, B> suffix(int from) {
        return new CompositePointFreeOptic<>(typed().suffix(from));
    }

    default <C, D> PointFreeOptic<S, T, C, D> andThen(PointFreeOptic<A, B, C, D> other) {
        return new CompositePointFreeOptic<>(typed().compose(other.typed()));
    }

    default <S2, T2> PointFreeOptic<S2, T2, A, B> castOuter(Type<S2> sourceType, Type<T2> targetType) {
        return new CompositePointFreeOptic<>(typed().castOuter(sourceType, targetType));
    }

    static <S, A> PointFreeOptic<S, S, A, A> lens(LensPath<S, A> path) {
        return lens(path, Types.variable("S"), Types.variable("A"));
    }

    static <S, A> PointFreeOptic<S, S, A, A> lens(
            LensPath<S, A> path, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return lens(path, Types.witness(sourceType), Types.witness(focusType));
    }

    static <S, A> PointFreeOptic<S, S, A, A> lens(
            LensPath<S, A> path, Type<S> sourceType, Type<A> focusType) {
        if (path.isIdentity()) {
            return new CompositePointFreeOptic<>(castOptic(TypedOptic.adapter(sourceType, sourceType)));
        }
        TypedOptic<S, S, A, A> optic = null;
        Type<?> current = sourceType;
        for (int i = 0; i < path.elements().size(); i++) {
            LensPath.Element pathElement = path.elements().get(i);
            Type<?> next = i == path.elements().size() - 1 ? focusType : Types.variable("LensPath" + i);
            TypedOptic<Object, Object, Object, Object> element = new TypedOptic<>(
                    Cartesian.Mu.TYPE_TOKEN,
                    castType(current),
                    castType(current),
                    castType(next),
                    castType(next),
                    new LensOpticElement(pathElement));
            optic = optic == null ? castOptic(element) : optic.compose(castOptic(element));
            current = next;
        }
        return new CompositePointFreeOptic<>(optic);
    }

    static <S> PointFreeOptic<S, S, S, S> adapter(TypeToken<S> type) {
        return adapter(Types.witness(type));
    }

    static <S, T> PointFreeOptic<S, T, S, T> adapter(TypeToken<S> sourceType, TypeToken<T> targetType) {
        return adapter(Types.witness(sourceType), Types.witness(targetType));
    }

    static <S> PointFreeOptic<S, S, S, S> adapter(Type<S> type) {
        return adapter(type, type);
    }

    static <S, T> PointFreeOptic<S, T, S, T> adapter(Type<S> sourceType, Type<T> targetType) {
        return new CompositePointFreeOptic<>(TypedOptic.adapter(sourceType, targetType));
    }

    static <S, A> PointFreeOptic<S, S, A, A> affine(Object key, PAffine<S, S, A, A> affine) {
        return affine(key, affine, Types.variable("S"), Types.variable("A"));
    }

    static <S, A> PointFreeOptic<S, S, A, A> affine(
            Object key, PAffine<S, S, A, A> affine, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return affine(key, affine, Types.witness(sourceType), Types.witness(focusType));
    }

    static <S, A> PointFreeOptic<S, S, A, A> affine(
            Object key, PAffine<S, S, A, A> affine, Type<S> sourceType, Type<A> focusType) {
        return new CompositePointFreeOptic<>(new TypedOptic<>(
                AffineP.Mu.TYPE_TOKEN,
                sourceType,
                sourceType,
                focusType,
                focusType,
                new AffineOpticElement<>(key, affine)));
    }

    static <S, A> PointFreeOptic<S, S, A, A> prism(Object key, PPrism<S, S, A, A> prism) {
        return prism(key, prism, Types.variable("S"), Types.variable("A"));
    }

    static <S, A> PointFreeOptic<S, S, A, A> prism(
            Object key, PPrism<S, S, A, A> prism, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return prism(key, prism, Types.witness(sourceType), Types.witness(focusType));
    }

    static <S, A> PointFreeOptic<S, S, A, A> prism(
            Object key, PPrism<S, S, A, A> prism, Type<S> sourceType, Type<A> focusType) {
        return new CompositePointFreeOptic<>(new TypedOptic<>(
                Choice.Mu.TYPE_TOKEN,
                sourceType,
                sourceType,
                focusType,
                focusType,
                new PrismOpticElement<>(key, prism)));
    }

    static <S, A> PointFreeOptic<S, S, A, A> fold(Object key, Fold<S, A> fold) {
        return fold(key, fold, Types.variable("S"), Types.variable("A"));
    }

    static <S, A> PointFreeOptic<S, S, A, A> fold(
            Object key, Fold<S, A> fold, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return fold(key, fold, Types.witness(sourceType), Types.witness(focusType));
    }

    static <S, A> PointFreeOptic<S, S, A, A> fold(
            Object key, Fold<S, A> fold, Type<S> sourceType, Type<A> focusType) {
        return new CompositePointFreeOptic<>(new TypedOptic<>(
                Monoidal.Mu.TYPE_TOKEN,
                sourceType,
                sourceType,
                focusType,
                focusType,
                new FoldOpticElement<>(key, fold)));
    }

    static <A, B> PointFreeOptic<Tuple2<A, B>, Tuple2<A, B>, Object, Object> product(ProductSide side) {
        return castPointFreeOptic(product(side, Types.variable("A"), Types.variable("B")));
    }

    static <A, B> PointFreeOptic<Tuple2<A, B>, Tuple2<A, B>, ?, ?> product(
            ProductSide side, TypeToken<A> firstType, TypeToken<B> secondType) {
        return product(side, Types.witness(firstType), Types.witness(secondType));
    }

    static <A, B> PointFreeOptic<Tuple2<A, B>, Tuple2<A, B>, ?, ?> product(
            ProductSide side, Type<A> firstType, Type<B> secondType) {
        return switch (side) {
            case FIRST -> new CompositePointFreeOptic<>(TypedOptic.proj1(firstType, secondType, firstType));
            case SECOND -> new CompositePointFreeOptic<>(TypedOptic.proj2(firstType, secondType, secondType));
        };
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, Object, Object> sum(SumSide side) {
        return castPointFreeOptic(sum(side, Types.variable("L"), Types.variable("R")));
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, ?, ?> sum(
            SumSide side, TypeToken<L> leftType, TypeToken<R> rightType) {
        return sum(side, Types.witness(leftType), Types.witness(rightType));
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, ?, ?> sum(
            SumSide side, Type<L> leftType, Type<R> rightType) {
        return switch (side) {
            case LEFT -> new CompositePointFreeOptic<>(TypedOptic.inj1(leftType, rightType, leftType));
            case RIGHT -> new CompositePointFreeOptic<>(TypedOptic.inj2(leftType, rightType, rightType));
        };
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, L, L> left(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return left(Types.witness(leftType), Types.witness(rightType));
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, L, L> left(
            Type<L> leftType,
            Type<R> rightType) {
        return new CompositePointFreeOptic<>(TypedOptic.inj1(leftType, rightType, leftType));
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, R, R> right(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return right(Types.witness(leftType), Types.witness(rightType));
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, R, R> right(
            Type<L> leftType,
            Type<R> rightType) {
        return new CompositePointFreeOptic<>(TypedOptic.inj2(leftType, rightType, rightType));
    }

    static <A> PointFreeOptic<List<A>, List<A>, A, A> list(TypeToken<A> elementType) {
        return list(Types.witness(elementType));
    }

    static <A> PointFreeOptic<List<A>, List<A>, A, A> list(Type<A> elementType) {
        return new CompositePointFreeOptic<>(TypedOptic.list(elementType, elementType));
    }

    static <A> PointFreeOptic<Maybe<A>, Maybe<A>, A, A> maybe(TypeToken<A> elementType) {
        return maybe(Types.witness(elementType));
    }

    static <A> PointFreeOptic<Maybe<A>, Maybe<A>, A, A> maybe(Type<A> elementType) {
        return new CompositePointFreeOptic<>(TypedOptic.maybe(elementType, elementType));
    }

    static PointFreeOptic<String, String, Character, Character> stringCharacters() {
        return new CompositePointFreeOptic<>(TypedOptic.stringCharacters());
    }

    static <E, A> PointFreeOptic<Validated<E, A>, Validated<E, A>, A, A> validatedValid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return validatedValid(Types.witness(errorType), Types.witness(valueType));
    }

    static <E, A> PointFreeOptic<Validated<E, A>, Validated<E, A>, A, A> validatedValid(
            Type<E> errorType,
            Type<A> valueType) {
        return new CompositePointFreeOptic<>(TypedOptic.validatedValid(errorType, valueType, valueType));
    }

    static <E, A> PointFreeOptic<Validated<E, A>, Validated<E, A>, E, E> validatedInvalid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return validatedInvalid(Types.witness(errorType), Types.witness(valueType));
    }

    static <E, A> PointFreeOptic<Validated<E, A>, Validated<E, A>, E, E> validatedInvalid(
            Type<E> errorType,
            Type<A> valueType) {
        return new CompositePointFreeOptic<>(TypedOptic.validatedInvalid(errorType, errorType, valueType));
    }

    static <S, A> PointFreeOptic<S, S, A, A> traversal(Object key, PTraversal<S, S, A, A> traversal) {
        return traversal(key, traversal, Types.variable("S"), Types.variable("A"));
    }

    static <S, A> PointFreeOptic<S, S, A, A> traversal(
            Object key, PTraversal<S, S, A, A> traversal, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return traversal(key, traversal, Types.witness(sourceType), Types.witness(focusType));
    }

    static <S, A> PointFreeOptic<S, S, A, A> traversal(
            Object key, PTraversal<S, S, A, A> traversal, Type<S> sourceType, Type<A> focusType) {
        return new CompositePointFreeOptic<>(new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                sourceType,
                sourceType,
                focusType,
                focusType,
                TraversalOpticElement.of(key, castTraversal(traversal))));
    }

    static <K, V> PointFreeOptic<Map<K, V>, Map<K, V>, V, V> mapValues(
            TypeToken<K> keyType, TypeToken<V> valueType) {
        return mapValues(Types.witness(keyType), Types.witness(valueType));
    }

    static <K, V> PointFreeOptic<Map<K, V>, Map<K, V>, V, V> mapValues(
            Type<K> keyType, Type<V> valueType) {
        Type<Map<K, V>> sourceType = Types.map(keyType, valueType);
        return new CompositePointFreeOptic<>(new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                sourceType,
                sourceType,
                valueType,
                valueType,
                MapOpticElement.values()));
    }

    static <K, V> PointFreeOptic<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> mapEntries(
            TypeToken<K> keyType, TypeToken<V> valueType) {
        return mapEntries(Types.witness(keyType), Types.witness(valueType));
    }

    static <K, V> PointFreeOptic<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> mapEntries(
            Type<K> keyType, Type<V> valueType) {
        Type<Map<K, V>> sourceType = Types.map(keyType, valueType);
        Type<Tuple2<K, V>> focusType = Types.and(keyType, valueType);
        return new CompositePointFreeOptic<>(new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                sourceType,
                sourceType,
                focusType,
                focusType,
                MapOpticElement.entries()));
    }

    static <K, A> PointFreeOptic<Tuple2<K, ?>, Tuple2<K, ?>, A, A> tagged(
            K tag, TypeToken<K> keyType, TypeToken<A> valueType) {
        Object2ObjectOpenHashMap<K, Type<?>> choices = new Object2ObjectOpenHashMap<>();
        Type<A> value = Types.witness(valueType);
        choices.put(tag, value);
        TaggedChoice.TaggedChoiceType<K> sourceType =
                Types.taggedChoiceType("tagged", Types.witness(keyType), choices);
        return new CompositePointFreeOptic<>(TypedOptic.tagged(sourceType, tag, value, value));
    }

    static <S, A extends S> PointFreeOptic<S, S, A, A> subtype(Class<S> sourceType, Class<A> subtype) {
        Type<S> source = Types.witness(sourceType);
        Type<A> focus = Types.witness(subtype);
        return new CompositePointFreeOptic<>(new TypedOptic<>(
                AffineP.Mu.TYPE_TOKEN,
                source,
                source,
                focus,
                focus,
                new SubtypeOpticElement(subtype)));
    }

    @SuppressWarnings("unchecked")
    private static <A> Type<A> castType(Type<?> type) {
        return (Type<A>) type;
    }

    @SuppressWarnings("unchecked")
    private static <S, T, A, B> TypedOptic<S, T, A, B> castOptic(TypedOptic<?, ?, ?, ?> optic) {
        return (TypedOptic<S, T, A, B>) optic;
    }

    @SuppressWarnings("unchecked")
    private static <S, T, A, B> PointFreeOptic<S, T, A, B> castPointFreeOptic(PointFreeOptic<?, ?, ?, ?> optic) {
        return (PointFreeOptic<S, T, A, B>) optic;
    }

    @SuppressWarnings("unchecked")
    private static <S, A> PTraversal<Object, Object, Object, Object> castTraversal(PTraversal<S, S, A, A> traversal) {
        return (PTraversal<Object, Object, Object, Object>) traversal;
    }
}

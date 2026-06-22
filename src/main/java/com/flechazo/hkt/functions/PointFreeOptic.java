package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.ProfunctorBound;
import com.flechazo.optics.Affine;
import com.flechazo.optics.Fold;
import com.flechazo.optics.Prism;
import com.flechazo.optics.Traversal;

import java.util.*;
import java.util.function.Function;

public sealed interface PointFreeOptic<S, T, A, B> permits CompositePointFreeOptic {
    List<PointFreeOpticElement> elements();

    T modify(Function<? super A, ? extends B> function, S source);

    default Maybe<PointFreeOpticTypes> types() {
        if (elements().isEmpty()) {
            return Maybe.none();
        }
        Maybe<PointFreeOpticTypes> outer = elements().getFirst().types();
        Maybe<PointFreeOpticTypes> inner = elements().getLast().types();
        if (outer.isEmpty() || inner.isEmpty()) {
            return Maybe.none();
        }
        return Maybe.some(outer.get().compose(inner.get()));
    }

    default Maybe<TypeRef<?>> sourceType() {
        return types().map(PointFreeOpticTypes::sourceType);
    }

    default Maybe<TypeRef<?>> targetType() {
        return types().map(PointFreeOpticTypes::targetType);
    }

    default Maybe<TypeRef<?>> focusType() {
        return types().map(PointFreeOpticTypes::focusType);
    }

    default Maybe<TypeRef<?>> replacementType() {
        return types().map(PointFreeOpticTypes::replacementType);
    }

    default Set<ProfunctorBound> bounds() {
        LinkedHashSet<ProfunctorBound> result = new LinkedHashSet<>();
        for (PointFreeOpticElement element : elements()) {
            result.addAll(element.bounds());
        }
        return Set.copyOf(result);
    }

    default PointFreeOpticElement outermost() {
        if (elements().isEmpty()) {
            throw new IllegalStateException("identity optic has no outermost element");
        }
        return elements().getFirst();
    }

    default boolean isIdentity() {
        return elements().isEmpty();
    }

    default int size() {
        return elements().size();
    }

    default int commonPrefixLength(PointFreeOptic<?, ?, ?, ?> other) {
        int size = Math.min(elements().size(), other.elements().size());
        for (int i = 0; i < size; i++) {
            if (!elements().get(i).sameOptic(other.elements().get(i))) {
                return i;
            }
        }
        return size;
    }

    default boolean sameElements(PointFreeOptic<?, ?, ?, ?> other) {
        return elements().size() == other.elements().size()
                && commonPrefixLength(other) == elements().size();
    }

    default boolean containsOnly(PointFreeOpticKind kind) {
        return !elements().isEmpty() && elements().stream().allMatch(element -> element.kind() == kind);
    }

    default boolean startsWith(PointFreeOpticKind kind) {
        return !elements().isEmpty() && outermost().kind() == kind;
    }

    default PointFreeOptic<S, T, Object, Object> prefix(int size) {
        if (size < 0 || size > elements().size()) {
            throw new IndexOutOfBoundsException(size);
        }
        return new CompositePointFreeOptic<>(elements().subList(0, size));
    }

    default PointFreeOptic<Object, Object, A, B> suffix(int from) {
        if (from < 0 || from > elements().size()) {
            throw new IndexOutOfBoundsException(from);
        }
        return new CompositePointFreeOptic<>(elements().subList(from, elements().size()));
    }

    default <C, D> PointFreeOptic<S, T, C, D> andThen(PointFreeOptic<A, B, C, D> other) {
        ArrayList<PointFreeOpticElement> next = new ArrayList<>(elements().size() + other.elements().size());
        next.addAll(elements());
        next.addAll(other.elements());
        Maybe<PointFreeOpticTypes> left = types();
        Maybe<PointFreeOpticTypes> right = other.types();
        Maybe<PointFreeOpticTypes> composed =
                left.isDefined() && right.isDefined() ? Maybe.some(left.get().compose(right.get())) : Maybe.none();
        return new CompositePointFreeOptic<>(next, composed);
    }

    static <S, A> PointFreeOptic<S, S, A, A> lens(LensPath<S, A> path) {
        return new CompositePointFreeOptic<>(path.elements().stream()
                .map(LensOpticElement::new)
                .map(element -> (PointFreeOpticElement) element)
                .toList());
    }

    static <S, A> PointFreeOptic<S, S, A, A> lens(
            LensPath<S, A> path, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return lens(path, sourceType.expr(), focusType.expr());
    }

    static <S, T, A, B> PointFreeOptic<S, T, A, B> lens(
            LensPath<S, A> path, TypeExpr sourceType, TypeExpr focusType) {
        return new CompositePointFreeOptic<>(path.elements().stream()
                .map(LensOpticElement::new)
                .map(element -> (PointFreeOpticElement) element)
                .toList(), Maybe.some(PointFreeOpticTypes.endomorphic(sourceType, focusType)));
    }

    static <S> PointFreeOptic<S, S, S, S> adapter(TypeRef<S> type) {
        return adapter(type.expr());
    }

    static <S, T> PointFreeOptic<S, T, S, T> adapter(TypeRef<S> sourceType, TypeRef<T> targetType) {
        return adapter(sourceType.expr(), targetType.expr());
    }

    static <S> PointFreeOptic<S, S, S, S> adapter(TypeExpr type) {
        return adapter(type, type);
    }

    static <S, T> PointFreeOptic<S, T, S, T> adapter(TypeExpr sourceType, TypeExpr targetType) {
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                new AdapterOpticElement(),
                new PointFreeOpticTypes(sourceType, targetType, sourceType, targetType))));
    }

    static <S, A> PointFreeOptic<S, S, A, A> affine(Object key, Affine<S, A> affine) {
        return new CompositePointFreeOptic<>(List.of(new AffineOpticElement(key, castAffine(affine))));
    }

    static <S, A> PointFreeOptic<S, S, A, A> affine(
            Object key, Affine<S, A> affine, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return affine(key, affine, sourceType.expr(), focusType.expr());
    }

    static <S, A> PointFreeOptic<S, S, A, A> affine(
            Object key, Affine<S, A> affine, TypeExpr sourceType, TypeExpr focusType) {
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                new AffineOpticElement(key, castAffine(affine)),
                PointFreeOpticTypes.endomorphic(sourceType, focusType))));
    }

    static <S, A> PointFreeOptic<S, S, A, A> prism(Object key, Prism<S, A> prism) {
        return new CompositePointFreeOptic<>(List.of(new PrismOpticElement(key, castPrism(prism))));
    }

    static <S, A> PointFreeOptic<S, S, A, A> prism(
            Object key, Prism<S, A> prism, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return prism(key, prism, sourceType.expr(), focusType.expr());
    }

    static <S, A> PointFreeOptic<S, S, A, A> prism(
            Object key, Prism<S, A> prism, TypeExpr sourceType, TypeExpr focusType) {
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                new PrismOpticElement(key, castPrism(prism)),
                PointFreeOpticTypes.endomorphic(sourceType, focusType))));
    }

    static <S, A> PointFreeOptic<S, S, A, A> fold(Object key, Fold<S, A> fold) {
        return new CompositePointFreeOptic<>(List.of(new FoldOpticElement(key, castFold(fold))));
    }

    static <S, A> PointFreeOptic<S, S, A, A> fold(
            Object key, Fold<S, A> fold, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return fold(key, fold, sourceType.expr(), focusType.expr());
    }

    static <S, A> PointFreeOptic<S, S, A, A> fold(
            Object key, Fold<S, A> fold, TypeExpr sourceType, TypeExpr focusType) {
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                new FoldOpticElement(key, castFold(fold)),
                PointFreeOpticTypes.endomorphic(sourceType, focusType))));
    }

    static <A, B> PointFreeOptic<Pair<A, B>, Pair<A, B>, Object, Object> product(ProductSide side) {
        return new CompositePointFreeOptic<>(List.of(new ProductOpticElement(side)));
    }

    static <A, B> PointFreeOptic<Pair<A, B>, Pair<A, B>, ?, ?> product(
            ProductSide side, TypeRef<A> firstType, TypeRef<B> secondType) {
        return product(side, firstType.expr(), secondType.expr());
    }

    static <A, B> PointFreeOptic<Pair<A, B>, Pair<A, B>, ?, ?> product(
            ProductSide side, TypeExpr firstType, TypeExpr secondType) {
        TypeExpr sourceType = TypeExpr.product(firstType, secondType);
        TypeExpr focusType = switch (side) {
            case FIRST -> firstType;
            case SECOND -> secondType;
        };
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                new ProductOpticElement(side),
                PointFreeOpticTypes.endomorphic(sourceType, focusType))));
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, Object, Object> sum(SumSide side) {
        return new CompositePointFreeOptic<>(List.of(new SumOpticElement(side)));
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, ?, ?> sum(
            SumSide side, TypeRef<L> leftType, TypeRef<R> rightType) {
        return sum(side, leftType.expr(), rightType.expr());
    }

    static <L, R> PointFreeOptic<Either<L, R>, Either<L, R>, ?, ?> sum(
            SumSide side, TypeExpr leftType, TypeExpr rightType) {
        TypeExpr sourceType = TypeExpr.sum(leftType, rightType);
        TypeExpr focusType = switch (side) {
            case LEFT -> leftType;
            case RIGHT -> rightType;
        };
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                new SumOpticElement(side),
                PointFreeOpticTypes.endomorphic(sourceType, focusType))));
    }

    static <A> PointFreeOptic<List<A>, List<A>, A, A> list(TypeRef<A> elementType) {
        return list(elementType.expr());
    }

    static <A> PointFreeOptic<List<A>, List<A>, A, A> list(TypeExpr elementType) {
        TypeExpr sourceType = TypeExpr.list(elementType);
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                TraversalOpticElement.list(),
                PointFreeOpticTypes.endomorphic(sourceType, elementType))));
    }

    static <S, A> PointFreeOptic<S, S, A, A> traversal(Object key, Traversal<S, A> traversal) {
        return new CompositePointFreeOptic<>(List.of(TraversalOpticElement.of(key, castTraversal(traversal))));
    }

    static <S, A> PointFreeOptic<S, S, A, A> traversal(
            Object key, Traversal<S, A> traversal, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return traversal(key, traversal, sourceType.expr(), focusType.expr());
    }

    static <S, A> PointFreeOptic<S, S, A, A> traversal(
            Object key, Traversal<S, A> traversal, TypeExpr sourceType, TypeExpr focusType) {
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                TraversalOpticElement.of(key, castTraversal(traversal)),
                PointFreeOpticTypes.endomorphic(sourceType, focusType))));
    }

    static <K, V> PointFreeOptic<Map<K, V>, Map<K, V>, V, V> mapValues(
            TypeRef<K> keyType, TypeRef<V> valueType) {
        return mapValues(keyType.expr(), valueType.expr());
    }

    static <K, V> PointFreeOptic<Map<K, V>, Map<K, V>, V, V> mapValues(
            TypeExpr keyType, TypeExpr valueType) {
        TypeExpr sourceType = TypeExpr.map(keyType, valueType);
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                MapOpticElement.values(),
                PointFreeOpticTypes.endomorphic(sourceType, valueType))));
    }

    static <K, V> PointFreeOptic<Map<K, V>, Map<K, V>, Pair<K, V>, Pair<K, V>> mapEntries(
            TypeRef<K> keyType, TypeRef<V> valueType) {
        return mapEntries(keyType.expr(), valueType.expr());
    }

    static <K, V> PointFreeOptic<Map<K, V>, Map<K, V>, Pair<K, V>, Pair<K, V>> mapEntries(
            TypeExpr keyType, TypeExpr valueType) {
        TypeExpr sourceType = TypeExpr.map(keyType, valueType);
        TypeExpr focusType = TypeExpr.product(keyType, valueType);
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                MapOpticElement.entries(),
                PointFreeOpticTypes.endomorphic(sourceType, focusType))));
    }

    static <K, A> PointFreeOptic<Pair<K, ?>, Pair<K, ?>, A, A> tagged(Object tag, TypeRef<K> keyType, TypeRef<A> valueType) {
        TypeRef<Pair<K, ?>> sourceWitness = TypeRef.parameterized(Pair.class, keyType, TypeRef.of(Object.class));
        TypeExpr sourceType = TypeExpr.taggedChoice(
                "tagged",
                keyType.expr(),
                Map.of(tag, valueType.expr()),
                sourceWitness);
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                new TaggedOpticElement(tag),
                PointFreeOpticTypes.endomorphic(sourceType, valueType.expr()))));
    }

    static <S, A extends S> PointFreeOptic<S, S, A, A> subtype(Class<S> sourceType, Class<A> subtype) {
        return new CompositePointFreeOptic<>(List.of(new TypedPointFreeOpticElement(
                new SubtypeOpticElement(subtype),
                PointFreeOpticTypes.endomorphic(TypeRef.of(sourceType).expr(), TypeRef.of(subtype).expr()))));
    }

    @SuppressWarnings("unchecked")
    private static <S, A> Affine<Object, Object> castAffine(Affine<S, A> affine) {
        return (Affine<Object, Object>) affine;
    }

    @SuppressWarnings("unchecked")
    private static <S, A> Prism<Object, Object> castPrism(Prism<S, A> prism) {
        return (Prism<Object, Object>) prism;
    }

    @SuppressWarnings("unchecked")
    private static <S, A> Fold<Object, Object> castFold(Fold<S, A> fold) {
        return (Fold<Object, Object>) fold;
    }

    @SuppressWarnings("unchecked")
    private static <S, A> Traversal<Object, Object> castTraversal(Traversal<S, A> traversal) {
        return (Traversal<Object, Object>) traversal;
    }
}

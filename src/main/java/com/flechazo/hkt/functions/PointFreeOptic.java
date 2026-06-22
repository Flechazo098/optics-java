package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.ProfunctorBound;

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

    static <S, T> PointFreeOptic<S, T, S, T> adapter(TypeExpr type) {
        return new CompositePointFreeOptic<>(List.of(), Maybe.some(PointFreeOpticTypes.endomorphic(type, type)));
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
}

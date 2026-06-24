package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.type.Type;
import com.flechazo.optics.Fold;

import java.util.function.Function;
import java.util.function.Predicate;

public sealed interface PointFreeFold<S, A> extends Fold<S, A> permits CompositePointFreeFold {
    TypedFold<S, A> typed();

    @Override
    default <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper, S source) {
        return typed().foldMap(monoid, mapper, source);
    }

    @Override
    default Maybe<PointFreeFold<S, A>> typedFold() {
        return Maybe.some(this);
    }

    default Type<S> sourceType() {
        return typed().sourceType();
    }

    default Type<A> focusType() {
        return typed().focusType();
    }

    default <B> PointFreeFold<S, B> andThen(PointFreeFold<A, B> other) {
        return new CompositePointFreeFold<>(typed().compose(other.typed()));
    }

    @Override
    default Fold<S, A> filtered(Predicate<? super A> predicate) {
        return new CompositePointFreeFold<>(typed().filtered(predicate));
    }

    @Override
    default Fold<S, A> plus(Fold<S, A> other) {
        return typedFold(other).map(right -> (Fold<S, A>) new CompositePointFreeFold<>(typed().plus(right.typed())))
                .orElseGet(() -> Fold.super.plus(other));
    }

    default boolean sameFold(PointFreeFold<?, ?> other) {
        return other != null && typed().sameFold(other.typed());
    }

    static <S, A> PointFreeFold<S, A> of(TypedFold<S, A> typed) {
        return new CompositePointFreeFold<>(typed);
    }

    static <S, T, A, B> PointFreeFold<S, A> fromOptic(
            PointFreeOptic<S, T, A, B> optic,
            Fold<S, A> executable) {
        return of(TypedFold.fromOptic(optic, executable));
    }

    static <S, A> PointFreeFold<S, A> opaque(
            Object key,
            Fold<S, A> fold,
            Type<S> sourceType,
            Type<A> focusType) {
        return of(TypedFold.opaque(key, fold, sourceType, focusType));
    }

    private static <S, A> Maybe<PointFreeFold<S, A>> typedFold(Fold<S, A> fold) {
        return fold.typedFold();
    }
}

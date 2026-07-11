package com.flechazo.optics.focus;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.*;

import java.util.function.Function;

public final class AffinePath<S, A> {
    private final PAffine<S, S, A, A> affine;

    private AffinePath(PAffine<S, S, A, A> affine) {
        this.affine = affine;
    }

    public static <S, A> AffinePath<S, A> of(PAffine<S, S, A, A> affine) {
        return new AffinePath<>(affine);
    }

    public PAffine<S, S, A, A> toAffine() {
        return affine;
    }

    public Fold<S, A> toFold() {
        return affine.asFold();
    }

    public Maybe<A> preview(S source) {
        return affine.getMaybe(source);
    }

    public S set(A value, S source) {
        return affine.set(value, source);
    }

    public S modify(Function<? super A, ? extends A> f, S source) {
        return affine.modify(f, source);
    }

    public <B> AffinePath<S, B> via(PLens<A, A, B, B> next) {
        return new AffinePath<>(affine.andThen(next));
    }

    public <B> AffinePath<S, B> via(PPrism<A, A, B, B> next) {
        return new AffinePath<>(affine.andThen(next));
    }

    public <B> AffinePath<S, B> via(PAffine<A, A, B, B> next) {
        return new AffinePath<>(affine.andThen(next));
    }

    public <B> TraversalPath<S, B> via(PTraversal<A, A, B, B> next) {
        return TraversalPath.of(affine.andThen(next));
    }
}

package com.flechazo.optics.focus;

import com.flechazo.optics.*;

import java.util.function.Function;

public final class FocusPath<S, A> {
    private final Lens<S, S, A, A> lens;

    private FocusPath(Lens<S, S, A, A> lens) {
        this.lens = lens;
    }

    public static <S, A> FocusPath<S, A> of(Lens<S, S, A, A> lens) {
        return new FocusPath<>(lens);
    }

    public Lens<S, S, A, A> toLens() {
        return lens;
    }

    public Getter<S, A> toGetter() {
        return lens.asGetter();
    }

    public Fold<S, A> toFold() {
        return lens.asFold();
    }

    public A get(S source) {
        return lens.get(source);
    }

    public S set(A value, S source) {
        return lens.set(value, source);
    }

    public S modify(Function<? super A, ? extends A> f, S source) {
        return lens.modify(f, source);
    }

    public <B> FocusPath<S, B> via(Lens<A, A, B, B> next) {
        return new FocusPath<>(lens.andThen(next));
    }

    public <B> FocusPath<S, B> via(FocusPath<A, B> next) {
        return via(next.toLens());
    }

    public <B> AffinePath<S, B> via(Prism<A, A, B, B> next) {
        return AffinePath.of(lens.andThen(next));
    }

    public <B> AffinePath<S, B> via(Affine<A, A, B, B> next) {
        return AffinePath.of(lens.andThen(next));
    }

    public <B> TraversalPath<S, B> via(Traversal<A, A, B, B> next) {
        return TraversalPath.of(lens.andThen(next));
    }

    public <B> TraversalPath<S, B> each(Traversal<A, A, B, B> traversal) {
        return via(traversal);
    }
}

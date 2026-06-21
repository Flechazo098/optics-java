package com.flechazo.optics.focus;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Fold;
import com.flechazo.optics.Lens;
import com.flechazo.optics.Prism;
import com.flechazo.optics.Traversal;

import java.util.List;
import java.util.function.Function;

public final class TraversalPath<S, A> {
    private final Traversal<S, A> traversal;

    private TraversalPath(Traversal<S, A> traversal) {
        this.traversal = traversal;
    }

    public static <S, A> TraversalPath<S, A> of(Traversal<S, A> traversal) {
        return new TraversalPath<>(traversal);
    }

    public Traversal<S, A> toTraversal() {
        return traversal;
    }

    public Fold<S, A> toFold() {
        return traversal.asFold();
    }

    public List<A> getAll(S source) {
        return traversal.getAll(source);
    }

    public Maybe<A> preview(S source) {
        return traversal.preview(source);
    }

    public S modify(Function<? super A, ? extends A> f, S source) {
        return traversal.modify(f, source);
    }

    public S set(A value, S source) {
        return traversal.set(value, source);
    }

    public <B> TraversalPath<S, B> via(Traversal<A, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }

    public <B> TraversalPath<S, B> via(Lens<A, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }

    public <B> TraversalPath<S, B> via(Prism<A, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }
}

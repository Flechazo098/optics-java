package com.flechazo.optics.focus;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Fold;
import com.flechazo.optics.PLens;
import com.flechazo.optics.PPrism;
import com.flechazo.optics.PTraversal;

import java.util.List;
import java.util.function.Function;

public final class TraversalPath<S, A> {
    private final PTraversal<S, S, A, A> traversal;

    private TraversalPath(PTraversal<S, S, A, A> traversal) {
        this.traversal = traversal;
    }

    public static <S, A> TraversalPath<S, A> of(PTraversal<S, S, A, A> traversal) {
        return new TraversalPath<>(traversal);
    }

    public PTraversal<S, S, A, A> toTraversal() {
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

    public <B> TraversalPath<S, B> via(PTraversal<A, A, B, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }

    public <B> TraversalPath<S, B> via(PLens<A, A, B, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }

    public <B> TraversalPath<S, B> via(PPrism<A, A, B, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }
}

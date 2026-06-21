package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.indexed.IndexedTraversal;

import java.util.Objects;

@FunctionalInterface
public interface Each<S, A> {
    Traversal<S, A> each();

    default <I> Maybe<IndexedTraversal<I, S, A>> eachWithIndex() {
        return Maybe.none();
    }

    default boolean supportsIndexed() {
        return eachWithIndex().isDefined();
    }

    static <S, A> Each<S, A> fromTraversal(Traversal<S, A> traversal) {
        Objects.requireNonNull(traversal, "traversal");
        return () -> traversal;
    }

    static <I, S, A> EachIndexed<I, S, A> fromIndexedTraversal(
            IndexedTraversal<I, S, A> traversal) {
        Objects.requireNonNull(traversal, "traversal");
        return () -> traversal;
    }
}

package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.indexed.IndexedTraversal;

@FunctionalInterface
public interface EachIndexed<I, S, A> extends Each<S, A> {
    IndexedTraversal<I, S, A> indexedTraversal();

    @Override
    default Traversal<S, S, A, A> each() {
        return indexedTraversal().asTraversal();
    }

    @Override
    @SuppressWarnings("unchecked")
    default <J> Maybe<IndexedTraversal<J, S, A>> eachWithIndex() {
        return Maybe.some((IndexedTraversal<J, S, A>) indexedTraversal());
    }
}

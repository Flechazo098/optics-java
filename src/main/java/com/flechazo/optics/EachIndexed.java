package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.indexed.IndexedTraversal;

/**
 * Provides both ordinary and indexed traversal over every focus of a source.
 *
 * @param <I> the index type
 * @param <S> the source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface EachIndexed<I, S, A> extends Each<S, A> {
    /**
     * Returns the indexed traversal used to enumerate the source focuses.
     *
     * @return the indexed traversal
     */
    IndexedTraversal<I, S, A> indexedTraversal();

    /**
     * Returns the traversal obtained by discarding each focus index.
     *
     * @return the unindexed traversal
     */
    @Override
    default Traversal<S, A> each() {
        return indexedTraversal().asTraversal();
    }

    /**
     * Returns this provider's indexed traversal with the requested compile-time index type.
     *
     * @param <J> the requested index type
     * @return a present value containing the indexed traversal
     */
    @Override
    @SuppressWarnings("unchecked")
    default <J> Maybe<IndexedTraversal<J, S, A>> eachWithIndex() {
        return Maybe.some((IndexedTraversal<J, S, A>) indexedTraversal());
    }
}

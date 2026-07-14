package com.flechazo.optics.focus;

import com.flechazo.optics.PLens;
import com.flechazo.optics.PTraversal;

/** Provides factory methods for reusable optic focus paths. */
public final class FocusPaths {
    private FocusPaths() {
    }

    /**
     * Creates a focus path from a monomorphic lens.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param lens the lens represented by the path
     * @return the resulting focus path
     */
    public static <S, A> FocusPath<S, A> of(PLens<S, S, A, A> lens) {
        return FocusPath.of(lens);
    }

    /**
     * Creates a traversal path from a monomorphic traversal.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param traversal the traversal represented by the path
     * @return the resulting traversal path
     */
    public static <S, A> TraversalPath<S, A> traversal(PTraversal<S, S, A, A> traversal) {
        return TraversalPath.of(traversal);
    }
}

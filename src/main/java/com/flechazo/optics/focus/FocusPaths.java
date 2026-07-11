package com.flechazo.optics.focus;

import com.flechazo.optics.PLens;
import com.flechazo.optics.PTraversal;

public final class FocusPaths {
    private FocusPaths() {
    }

    public static <S, A> FocusPath<S, A> of(PLens<S, S, A, A> lens) {
        return FocusPath.of(lens);
    }

    public static <S, A> TraversalPath<S, A> traversal(PTraversal<S, S, A, A> traversal) {
        return TraversalPath.of(traversal);
    }
}

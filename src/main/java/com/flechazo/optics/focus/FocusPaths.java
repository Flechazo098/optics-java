package com.flechazo.optics.focus;

import com.flechazo.optics.Lens;
import com.flechazo.optics.Traversal;

public final class FocusPaths {
    private FocusPaths() {
    }

    public static <S, A> FocusPath<S, A> of(Lens<S, A> lens) {
        return FocusPath.of(lens);
    }

    public static <S, A> TraversalPath<S, A> traversal(Traversal<S, A> traversal) {
        return TraversalPath.of(traversal);
    }
}

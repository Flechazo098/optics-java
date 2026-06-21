package com.flechazo.optics.util;

import com.flechazo.optics.Traversal;

import java.util.List;

public final class ListTraversals {
    private ListTraversals() {
    }

    public static <A> Traversal<List<A>, A> forList() {
        return Traversals.forList();
    }
}

package com.flechazo.optics.util;

import com.flechazo.optics.indexed.IndexedTraversal;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public final class IndexedTraversals {
    private IndexedTraversals() {
    }

    public static <I, S, A> S imodify(
            IndexedTraversal<I, S, A> traversal, BiFunction<? super I, ? super A, ? extends A> f, S source) {
        return traversal.imodify(f, source);
    }

    public static <A> IndexedTraversal<Integer, List<A>, A> forList() {
        return IndexedTraversal.forList();
    }

    public static <K, V> IndexedTraversal<K, Map<K, V>, V> forMapValues() {
        return IndexedTraversal.forMapValues();
    }
}

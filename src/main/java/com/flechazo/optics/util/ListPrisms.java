package com.flechazo.optics.util;

import com.flechazo.hkt.Either;
import com.flechazo.optics.Affine;
import com.flechazo.optics.PAffine;
import com.flechazo.optics.PPrism;
import com.flechazo.optics.Prism;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides optics and construction operations for lists.
 */
public final class ListPrisms {
    private ListPrisms() {
    }

    /**
     * Creates an affine that focuses the first element of a non-empty list.
     *
     * @param <A> the element type
     * @return an affine with no focus for an empty list
     */
    public static <A> Affine<List<A>, A> head() {
        return Affine.from(PAffine.of(
                list -> list.isEmpty() ? Either.left(list) : Either.right(list.getFirst()),
                (source, next) -> {
                    if (source.isEmpty()) {
                        return source;
                    }
                    ArrayList<A> result = new ArrayList<>(source);
                    result.set(0, next);
                    return Collections.unmodifiableList(result);
                }));
    }

    /**
     * Creates a prism that selects non-empty lists.
     *
     * @param <A> the element type
     * @return a prism whose focus is the selected list
     */
    public static <A> Prism<List<A>, List<A>> nonEmpty() {
        return Prism.from(ListPrisms.pNonEmpty());
    }

    /**
     * Creates a polymorphic prism that selects non-empty lists.
     *
     * @param <A> the source element type
     * @param <B> the replacement element type
     * @return a prism whose focus is the selected list
     */
    public static <A, B> PPrism<List<A>, List<B>, List<A>, List<B>> pNonEmpty() {
        return PPrism.of(
                list -> list.isEmpty() ? Either.left(List.of()) : Either.right(list),
                list -> list);
    }

    /**
     * Creates a prism that selects empty lists.
     *
     * @param <A> the element type
     * @return a prism whose focus is an empty list
     */
    public static <A> Prism<List<A>, List<A>> empty() {
        return Prism.from(PPrism.of(
                list -> list.isEmpty() ? Either.right(list) : Either.left(list),
                ignored -> List.of()));
    }

    /**
     * Creates a list containing a new first element followed by the supplied tail.
     *
     * @param <A> the element type
     * @param head the first element
     * @param tail the elements following the first element
     * @return an unmodifiable list containing {@code head} followed by {@code tail}
     */
    public static <A> List<A> prepend(A head, List<A> tail) {
        ArrayList<A> result = new ArrayList<>(tail.size() + 1);
        result.add(head);
        result.addAll(tail);
        return Collections.unmodifiableList(result);
    }
}

package com.flechazo.optics.util;

import com.flechazo.hkt.Either;
import com.flechazo.optics.PAffine;
import com.flechazo.optics.PPrism;
import com.flechazo.optics.Affine;
import com.flechazo.optics.Prism;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ListPrisms {
    private ListPrisms() {
    }

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

    public static <A> Prism<List<A>, List<A>> nonEmpty() {
        return Prism.from(ListPrisms.<A, A>pNonEmpty());
    }

    public static <A, B> PPrism<List<A>, List<B>, List<A>, List<B>> pNonEmpty() {
        return PPrism.of(
                list -> list.isEmpty() ? Either.left(List.of()) : Either.right(list),
                list -> list);
    }

    public static <A> Prism<List<A>, List<A>> empty() {
        return Prism.from(PPrism.of(
                list -> list.isEmpty() ? Either.right(list) : Either.left(list),
                ignored -> List.of()));
    }

    public static <A> List<A> prepend(A head, List<A> tail) {
        ArrayList<A> result = new ArrayList<>(tail.size() + 1);
        result.add(head);
        result.addAll(tail);
        return Collections.unmodifiableList(result);
    }
}

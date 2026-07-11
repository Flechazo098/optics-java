package com.flechazo.optics.util;

import com.flechazo.hkt.Either;
import com.flechazo.optics.PAffine;
import com.flechazo.optics.PPrism;

import java.util.ArrayList;
import java.util.List;

public final class ListPrisms {
    private ListPrisms() {
    }

    public static <A> PAffine<List<A>, List<A>, A, A> head() {
        return PAffine.of(
                list -> list.isEmpty() ? Either.left(list) : Either.right(list.getFirst()),
                (source, next) -> {
                    if (source.isEmpty()) {
                        return source;
                    }
                    ArrayList<A> result = new ArrayList<>(source);
                    result.set(0, next);
                    return List.copyOf(result);
                });
    }

    public static <A> PPrism<List<A>, List<A>, List<A>, List<A>> nonEmpty() {
        return PPrism.of(list -> list.isEmpty() ? Either.left(list) : Either.right(list), List::copyOf);
    }

    public static <A> PPrism<List<A>, List<A>, List<A>, List<A>> empty() {
        return PPrism.of(list -> list.isEmpty() ? Either.right(list) : Either.left(list), ignored -> List.of());
    }

    public static <A> List<A> prepend(A head, List<A> tail) {
        ArrayList<A> result = new ArrayList<>(tail.size() + 1);
        result.add(head);
        result.addAll(tail);
        return List.copyOf(result);
    }
}

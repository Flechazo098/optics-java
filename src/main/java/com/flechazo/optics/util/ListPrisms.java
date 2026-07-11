package com.flechazo.optics.util;

import com.flechazo.hkt.Either;
import com.flechazo.optics.Affine;
import com.flechazo.optics.Prism;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ListPrisms {
    private ListPrisms() {
    }

    public static <A> Affine<List<A>, List<A>, A, A> head() {
        return Affine.of(
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

    public static <A> Optional<A> headOptional(List<A> list) {
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.getFirst());
    }

    public static <A> Prism<List<A>, List<A>, List<A>, List<A>> nonEmpty() {
        return Prism.of(list -> list.isEmpty() ? Either.left(list) : Either.right(list), List::copyOf);
    }

    public static <A> Optional<List<A>> nonEmptyOptional(List<A> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(list));
    }

    public static <A> Prism<List<A>, List<A>, List<A>, List<A>> empty() {
        return Prism.of(list -> list.isEmpty() ? Either.right(list) : Either.left(list), ignored -> List.of());
    }

    public static <A> List<A> prepend(A head, List<A> tail) {
        ArrayList<A> result = new ArrayList<>(tail.size() + 1);
        result.add(head);
        result.addAll(tail);
        return List.copyOf(result);
    }
}

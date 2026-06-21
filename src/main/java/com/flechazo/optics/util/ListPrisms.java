package com.flechazo.optics.util;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Prism;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ListPrisms {
    private ListPrisms() {
    }

    public static <A> Prism<List<A>, A> head() {
        return Prism.of(
                list -> list.isEmpty() ? Maybe.none() : Maybe.some(list.getFirst()),
                List::of);
    }

    public static <A> Optional<A> headOptional(List<A> list) {
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.getFirst());
    }

    public static <A> Prism<List<A>, List<A>> nonEmpty() {
        return Prism.of(list -> list.isEmpty() ? Maybe.none() : Maybe.some(list), List::copyOf);
    }

    public static <A> Optional<List<A>> nonEmptyOptional(List<A> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(list));
    }

    public static <A> Prism<List<A>, List<A>> empty() {
        return Prism.of(list -> list.isEmpty() ? Maybe.some(list) : Maybe.none(), ignored -> List.of());
    }

    public static <A> List<A> prepend(A head, List<A> tail) {
        ArrayList<A> result = new ArrayList<>(tail.size() + 1);
        result.add(head);
        result.addAll(tail);
        return List.copyOf(result);
    }
}

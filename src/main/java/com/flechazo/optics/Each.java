package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.indexed.IndexedTraversal;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface Each<S, A> {
    Traversal<S, S, A, A> each();

    default <I> Maybe<IndexedTraversal<I, S, A>> eachWithIndex() {
        return Maybe.none();
    }

    default boolean supportsIndexed() {
        return eachWithIndex().isDefined();
    }

    static <S, A> Each<S, A> fromTraversal(Traversal<S, S, A, A> traversal) {
        Objects.requireNonNull(traversal, "traversal");
        return () -> traversal;
    }

    static <I, S, A> EachIndexed<I, S, A> fromIndexedTraversal(
            IndexedTraversal<I, S, A> traversal) {
        Objects.requireNonNull(traversal, "traversal");
        return () -> traversal;
    }

    static <A> EachIndexed<Integer, List<A>, A> listEach() {
        return IndexedTraversal::forList;
    }

    static <K, V> EachIndexed<K, Map<K, V>, V> mapValueEach() {
        return IndexedTraversal::forMapValues;
    }

    static <A> Each<Set<A>, A> setEach() {
        return () ->
                new Traversal<>() {
                    @Override
                    public <F extends K1> App<F, Set<A>> modifyF(
                            Function<A, App<F, A>> f, Set<A> source, Applicative<F, ?> applicative) {
                        App<F, LinkedHashSet<A>> built = applicative.of(new LinkedHashSet<>());
                        for (A value : source) {
                            built =
                                    applicative.map2(
                                            built,
                                            f.apply(value),
                                            (set, next) -> {
                                                LinkedHashSet<A> copy = new LinkedHashSet<>(set);
                                                copy.add(next);
                                                return copy;
                                            });
                        }
                        return applicative.map(set -> set, built);
                    }
                };
    }

    static <A> Each<Maybe<A>, A> maybeEach() {
        return () ->
                new Traversal<>() {
                    @Override
                    public <F extends K1> App<F, Maybe<A>> modifyF(
                            Function<A, App<F, A>> f, Maybe<A> source, Applicative<F, ?> applicative) {
                        return source.isDefined()
                                ? applicative.map(Maybe::some, f.apply(source.get()))
                                : applicative.of(Maybe.none());
                    }
                };
    }

    @SuppressWarnings("unchecked")
    static <A> EachIndexed<Integer, A[], A> arrayEach(Class<A> componentType) {
        return () ->
                new IndexedTraversal<>() {
                    @Override
                    public <F extends K1> App<F, A[]> imodifyF(
                            BiFunction<Integer, A, App<F, A>> f,
                            A[] source,
                            Applicative<F, ?> applicative) {
                        App<F, List<A>> built = applicative.of(new ArrayList<>(source.length));
                        for (int i = 0; i < source.length; i++) {
                            final int index = i;
                            built =
                                    applicative.map2(
                                            built,
                                            f.apply(index, source[index]),
                                            (list, value) -> {
                                                ArrayList<A> next = new ArrayList<>(list);
                                                next.add(value);
                                                return next;
                                            });
                        }
                        return applicative.map(
                                values -> values.toArray((A[]) Array.newInstance(componentType, values.size())),
                                built);
                    }
                };
    }

    static <A> Traversal<List<A>, List<A>, A, A> listTraversal() {
        return Each.<A>listEach().each();
    }

    static <K, V> Traversal<Map<K, V>, Map<K, V>, V, V> mapValueTraversal() {
        return Each.<K, V>mapValueEach().each();
    }

    static <A> Traversal<Set<A>, Set<A>, A, A> setTraversal() {
        return Each.<A>setEach().each();
    }
}

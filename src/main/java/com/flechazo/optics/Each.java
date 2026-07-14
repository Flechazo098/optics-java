package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.business.data.Chain;
import com.flechazo.optics.indexed.IndexedTraversal;
import com.flechazo.optics.internal.OpticPrograms;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Provides a traversal over every focus of a source.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface Each<S, A> {
    /**
     * Returns the traversal over every focus.
     *
     * @return the traversal
     */
    Traversal<S, A> each();

    /**
     * Returns an indexed traversal when this provider preserves indexes.
     *
     * @param <I> the requested index type
     * @return the indexed traversal, or an empty value when indexes are unavailable
     */
    default <I> Maybe<IndexedTraversal<I, S, A>> eachWithIndex() {
        return Maybe.none();
    }

    /**
     * Determines whether an indexed traversal is available.
     *
     * @return {@code true} when indexes are supported
     */
    default boolean supportsIndexed() {
        return eachWithIndex().isDefined();
    }

    /**
     * Creates an {@code Each} provider from a traversal.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param traversal the traversal to expose
     * @return the resulting provider
     */
    static <S, A> Each<S, A> fromTraversal(Traversal<S, A> traversal) {
        Objects.requireNonNull(traversal, "traversal");
        return () -> traversal;
    }

    /**
     * Creates an indexed {@code Each} provider from an indexed traversal.
     *
     * @param <I> the index type
     * @param <S> the source type
     * @param <A> the focus type
     * @param traversal the indexed traversal to expose
     * @return the resulting provider
     */
    static <I, S, A> EachIndexed<I, S, A> fromIndexedTraversal(
            IndexedTraversal<I, S, A> traversal) {
        Objects.requireNonNull(traversal, "traversal");
        return () -> traversal;
    }

    /**
     * Returns a provider traversing list elements by integer index.
     *
     * @param <A> the element type
     * @return the list provider
     */
    static <A> EachIndexed<Integer, List<A>, A> listEach() {
        return IndexedTraversal::forList;
    }

    /**
     * Returns a provider traversing map values by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the map-value provider
     */
    static <K, V> EachIndexed<K, Map<K, V>, V> mapValueEach() {
        return IndexedTraversal::forMapValues;
    }

    /**
     * Returns a provider traversing set elements in iteration order.
     *
     * @param <A> the element type
     * @return the set provider
     */
    static <A> Each<Set<A>, A> setEach() {
        return () ->
                Traversal.from(new Traversal<>() {
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
                });
    }

    /**
     * Returns a provider traversing the value of a nonempty optional value.
     *
     * @param <A> the value type
     * @return the optional-value provider
     */
    static <A> Each<Maybe<A>, A> maybeEach() {
        return () ->
                Traversal.from(new Traversal<>() {
                    @Override
                    public <F extends K1> App<F, Maybe<A>> modifyF(
                            Function<A, App<F, A>> f, Maybe<A> source, Applicative<F, ?> applicative) {
                        return source.isDefined()
                                ? applicative.map(Maybe::some, f.apply(source.get()))
                                : applicative.of(Maybe.none());
                    }
                });
    }

    /**
     * Returns a provider traversing array elements by integer index.
     *
     * @param <A> the component type
     * @param componentType the runtime component class
     * @return the array provider
     */
    @SuppressWarnings("unchecked")
    static <A> EachIndexed<Integer, A[], A> arrayEach(Class<A> componentType) {
        return () -> {
            IndexedTraversal<Integer, A[], A> direct = new IndexedTraversal<>() {
                @Override
                public <F extends K1> App<F, A[]> imodifyF(
                        BiFunction<Integer, A, App<F, A>> f,
                        A[] source,
                        Applicative<F, ?> applicative) {
                    App<F, Chain<A>> built = applicative.of(Chain.empty());
                    for (int i = 0; i < source.length; i++) {
                        final int index = i;
                        built =
                                applicative.map2(
                                        built,
                                        f.apply(index, source[index]),
                                        Chain::append);
                    }
                    return applicative.map(
                            values -> values.toList().toArray((A[]) Array.newInstance(componentType, source.length)),
                            built);
                }
            };
            return OpticPrograms.indexedTraversal(
                    direct, OpticPrograms.structured("indexedArrayTraversal", componentType));
        };
    }

}

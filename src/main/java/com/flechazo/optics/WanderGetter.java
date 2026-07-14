package com.flechazo.optics;

import com.flechazo.hkt.tuple.Tuple2;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * Represents a serializable function that enumerates all focuses of a traversal.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface WanderGetter<S, A> extends Function<S, Iterable<? extends A>>, Serializable {
    /**
     * Returns the elements of a list in encounter order.
     *
     * @param <A> the element type
     * @param source the list whose elements are returned
     * @return {@code source}
     */
    static <A> List<A> list(List<A> source) {
        return source;
    }

    /**
     * Returns the elements of a set in its iteration order.
     *
     * @param <A> the element type
     * @param source the set whose elements are returned
     * @return an iterable view of {@code source}
     */
    static <A> Iterable<A> set(Set<A> source) {
        return source;
    }

    /**
     * Returns the values of a map in the map's value iteration order.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param source the map whose values are returned
     * @return an iterable view of the map values
     */
    static <K, V> Iterable<V> mapValues(Map<K, V> source) {
        return source.values();
    }

    /**
     * Returns the entries of a map as ordered key-value tuples.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param source the map whose entries are returned
     * @return an iterable that yields one tuple for each map entry
     */
    static <K, V> Iterable<Tuple2<K, V>> mapEntries(Map<K, V> source) {
        return () -> new Iterator<>() {
            private final Iterator<Map.Entry<K, V>> entries = source.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return entries.hasNext();
            }

            @Override
            public Tuple2<K, V> next() {
                Map.Entry<K, V> entry = entries.next();
                return Tuple2.of(entry.getKey(), entry.getValue());
            }
        };
    }

    /**
     * Returns the elements of an array in ascending index order.
     *
     * @param <A> the component type
     * @param source the array whose elements are returned
     * @return an iterable over the array elements
     */
    static <A> Iterable<A> array(A[] source) {
        return () -> new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < source.length;
            }

            @Override
            public A next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return source[index++];
            }
        };
    }

    /**
     * Returns the UTF-16 code units of a string as characters in index order.
     *
     * @param source the string whose characters are returned
     * @return an iterable over the string characters
     */
    static Iterable<Character> stringCharacters(String source) {
        return () -> new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < source.length();
            }

            @Override
            public Character next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return source.charAt(index++);
            }
        };
    }
}

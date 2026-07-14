package com.flechazo.optics;

import com.flechazo.hkt.tuple.Tuple2;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Represents a serializable function that rebuilds a traversal source from replacement focuses.
 *
 * @param <S> the source and rebuilt source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface WanderRebuilder<S, A> extends BiFunction<S, List<A>, S>, Serializable {
    /**
     * Returns the replacement values as the rebuilt list.
     *
     * @param <A> the element type
     * @param source the original list
     * @param values the replacement elements in encounter order
     * @return {@code values}
     */
    static <A> List<A> list(List<A> source, List<A> values) {
        return values;
    }

    /**
     * Creates an insertion-ordered set from replacement values.
     *
     * @param <A> the element type
     * @param source the original set
     * @param values the replacement elements in encounter order
     * @return an unmodifiable insertion-ordered set of the replacement values
     */
    static <A> Set<A> set(Set<A> source, List<A> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    /**
     * Rebuilds a map by associating its existing keys with replacement values in key iteration
     * order.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param source the map providing keys and their order
     * @param values the replacement values
     * @return an unmodifiable map containing the existing keys and replacement values
     * @throws IllegalArgumentException if the number of values differs from the number of keys
     */
    static <K, V> Map<K, V> mapValues(Map<K, V> source, List<V> values) {
        if (source.size() != values.size()) {
            throw new IllegalArgumentException("Map value count changed during traversal rebuild");
        }
        LinkedHashMap<K, V> result = new LinkedHashMap<>(source.size());
        int index = 0;
        for (K key : source.keySet()) {
            result.put(key, values.get(index++));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Rebuilds a map from replacement key-value tuples in encounter order.
     *
     * <p>When a key occurs more than once, its last value is retained at the key's first insertion
     * position.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param source the original map
     * @param values the replacement entries
     * @return an unmodifiable insertion-ordered map containing the replacement entries
     */
    static <K, V> Map<K, V> mapEntries(Map<K, V> source, List<Tuple2<K, V>> values) {
        LinkedHashMap<K, V> result = new LinkedHashMap<>(values.size());
        for (Tuple2<K, V> value : values) {
            result.put(value.first(), value.second());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Creates an array of the original component type containing the replacement values.
     *
     * @param <A> the component type
     * @param source the array that determines the runtime component type
     * @param values the replacement elements
     * @return a new array containing the replacement elements
     * @throws ArrayStoreException if a replacement value is not assignable to the component type
     */
    @SuppressWarnings("unchecked")
    static <A> A[] array(A[] source, List<A> values) {
        A[] result = (A[]) Array.newInstance(source.getClass().getComponentType(), values.size());
        return values.toArray(result);
    }

    /**
     * Creates a string from replacement UTF-16 characters.
     *
     * @param source the original string
     * @param values the replacement characters
     * @return a string containing the replacement characters in encounter order
     */
    static String stringCharacters(String source, List<Character> values) {
        StringBuilder result = new StringBuilder(values.size());
        values.forEach(result::append);
        return result.toString();
    }
}

package com.flechazo.optics;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Represents a serializable function that rebuilds an affine source with a replacement focus.
 *
 * @param <S> the source type
 * @param <A> the replacement focus type
 * @param <T> the rebuilt source type
 */
@FunctionalInterface
public interface AffineRebuilder<S, A, T> extends BiFunction<S, A, T>, Serializable {
    /**
     * Replaces the value associated with a map key when that key is present.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param source the map to rebuild
     * @param key the key whose value is replaced
     * @param value the replacement value
     * @return an unmodifiable map containing the replacement, or {@code source} when the key is
     * absent
     */
    static <K, V> Map<K, V> mapValue(Map<K, V> source, K key, V value) {
        if (!source.containsKey(key)) {
            return source;
        }
        LinkedHashMap<K, V> result = new LinkedHashMap<>(source);
        result.put(key, value);
        return Collections.unmodifiableMap(result);
    }

    /**
     * Replaces the element at a list index when that index is in range.
     *
     * @param <A> the element type
     * @param source the list to rebuild
     * @param index the zero-based element index
     * @param value the replacement element
     * @return an unmodifiable list containing the replacement, or {@code source} when the index is
     * out of range
     */
    static <A> List<A> listIndex(List<A> source, int index, A value) {
        if (index < 0 || index >= source.size()) {
            return source;
        }
        ArrayList<A> result = new ArrayList<>(source);
        result.set(index, value);
        return Collections.unmodifiableList(result);
    }
}

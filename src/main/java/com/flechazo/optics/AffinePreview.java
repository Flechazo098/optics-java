package com.flechazo.optics;

import com.flechazo.hkt.Either;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a serializable partial match used to preview an affine focus.
 *
 * @param <S> the input source type
 * @param <T> the nonmatching rebuilt source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface AffinePreview<S, T, A> extends Function<S, Either<T, A>>, Serializable {
    /**
     * Returns the value associated with a map key, or the original map when the key is absent.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param source the map to inspect
     * @param key the key whose value is requested
     * @return a right value containing the mapped value when present, otherwise a left value
     * containing {@code source}
     */
    static <K, V> Either<Map<K, V>, V> mapValue(Map<K, V> source, K key) {
        return source.containsKey(key) ? Either.right(source.get(key)) : Either.left(source);
    }

    /**
     * Returns the element at a list index, or the original list when the index is out of range.
     *
     * @param <A> the element type
     * @param source the list to inspect
     * @param index the zero-based element index
     * @return a right value containing the indexed element when present, otherwise a left value
     * containing {@code source}
     */
    static <A> Either<List<A>, A> listIndex(List<A> source, int index) {
        return index >= 0 && index < source.size() ? Either.right(source.get(index)) : Either.left(source);
    }
}

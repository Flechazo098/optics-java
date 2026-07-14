package com.flechazo.optics;

import com.flechazo.hkt.Maybe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Provides lenses that model presence or absence at an index.
 *
 * @param <S> the source type
 * @param <I> the index type
 * @param <A> the indexed value type
 */
@FunctionalInterface
public interface At<S, I, A> {
    /**
     * Returns a lens focusing on the optional value at an index.
     *
     * @param index the index to focus
     * @return a lens whose focus is present when the index exists
     */
    Lens<S, Maybe<A>> at(I index);

    /**
     * Gets the optional value at an index.
     *
     * @param index the index to read
     * @param source the source to inspect
     * @return the indexed value when present
     */
    default Maybe<A> get(I index, S source) {
        return at(index).get(source);
    }

    /**
     * Sets the presence or absence of a value at an index.
     *
     * @param index the index to update
     * @param value the optional replacement value
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    default S set(I index, Maybe<A> value, S source) {
        return at(index).set(value, source);
    }

    /**
     * Inserts a value or replaces the existing value at an index.
     *
     * @param index the index to update
     * @param value the replacement value
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    default S insertOrUpdate(I index, A value, S source) {
        return set(index, Maybe.some(value), source);
    }

    /**
     * Removes the value at an index when present.
     *
     * @param index the index to remove
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    default S remove(I index, S source) {
        return set(index, Maybe.none(), source);
    }

    /**
     * Transforms the value at an index when present.
     *
     * @param index the index to update
     * @param f the value transformation
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    default S modify(I index, Function<? super A, ? extends A> f, S source) {
        return at(index).modify(value -> value.map(f), source);
    }

    /**
     * Determines whether an index is present.
     *
     * @param index the index to test
     * @param source the source to inspect
     * @return {@code true} when the index is present
     */
    default boolean contains(I index, S source) {
        return get(index, source).isDefined();
    }

    /**
     * Returns indexed-presence lenses for maps.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the map {@code At} instance
     */
    static <K, V> At<Map<K, V>, K, V> mapAt() {
        return key ->
                Lens.of(
                        source -> source.containsKey(key) ? Maybe.some(source.get(key)) : Maybe.none(),
                        (source, value) -> {
                            LinkedHashMap<K, V> copy = new LinkedHashMap<>(source);
                            if (value.isDefined()) {
                                copy.put(key, value.get());
                            } else {
                                copy.remove(key);
                            }
                            return copy;
                        });
    }

    /**
     * Returns indexed-presence lenses for lists.
     *
     * @param <A> the element type
     * @return the list {@code At} instance
     */
    static <A> At<List<A>, Integer, A> listAt() {
        return index ->
                Lens.of(
                        source -> index >= 0 && index < source.size() ? Maybe.some(source.get(index)) : Maybe.none(),
                        (source, value) -> {
                            ArrayList<A> copy = new ArrayList<>(source);
                            if (value.isDefined()) {
                                if (index < 0) {
                                    return source;
                                }
                                if (index < copy.size()) {
                                    copy.set(index, value.get());
                                } else if (index == copy.size()) {
                                    copy.add(value.get());
                                }
                            } else if (index >= 0 && index < copy.size()) {
                                copy.remove((int) index);
                            }
                            return copy;
                        });
    }
}

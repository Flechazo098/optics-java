package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Provides traversals focused at a particular index without inserting or removing values.
 *
 * @param <S> the source type
 * @param <I> the index type
 * @param <A> the indexed value type
 */
@FunctionalInterface
public interface Ixed<S, I, A> {
    /**
     * Returns a traversal containing the indexed value when present.
     *
     * @param index the index to focus
     * @return the indexed traversal
     */
    Traversal<S, A> ix(I index);

    /**
     * Returns indexed traversals for map values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the map {@code Ixed} instance
     */
    static <K, V> Ixed<Map<K, V>, K, V> mapIxed() {
        return key ->
                Traversal.from(new Traversal<>() {
                    @Override
                    public <F extends K1> App<F, Map<K, V>> modifyF(
                            Function<V, App<F, V>> f, Map<K, V> source, Applicative<F, ?> applicative) {
                        if (!source.containsKey(key)) {
                            return applicative.of(source);
                        }
                        return applicative.map(
                                value -> {
                                    LinkedHashMap<K, V> copy = new LinkedHashMap<>(source);
                                    copy.put(key, value);
                                    return copy;
                                },
                                f.apply(source.get(key)));
                    }
                });
    }

    /**
     * Returns indexed traversals for list elements.
     *
     * @param <A> the element type
     * @return the list {@code Ixed} instance
     */
    static <A> Ixed<List<A>, Integer, A> listIxed() {
        return index ->
                Traversal.from(new Traversal<>() {
                    @Override
                    public <F extends K1> App<F, List<A>> modifyF(
                            Function<A, App<F, A>> f, List<A> source, Applicative<F, ?> applicative) {
                        if (index < 0 || index >= source.size()) {
                            return applicative.of(source);
                        }
                        return applicative.map(
                                value -> {
                                    ArrayList<A> copy = new ArrayList<>(source);
                                    copy.set(index, value);
                                    return copy;
                                },
                                f.apply(source.get(index)));
                    }
                });
    }
}

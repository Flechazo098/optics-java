package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface Ixed<S, I, A> {
    Traversal<S, A> ix(I index);

    static <K, V> Ixed<Map<K, V>, K, V> mapIxed() {
        return key ->
                new Traversal<>() {
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
                };
    }

    static <A> Ixed<List<A>, Integer, A> listIxed() {
        return index ->
                new Traversal<>() {
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
                };
    }
}

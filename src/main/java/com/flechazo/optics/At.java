package com.flechazo.optics;

import com.flechazo.hkt.Maybe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface At<S, I, A> {
    Lens<S, Maybe<A>> at(I index);

    default Maybe<A> get(I index, S source) {
        return at(index).get(source);
    }

    default S set(I index, Maybe<A> value, S source) {
        return at(index).set(value, source);
    }

    default S insertOrUpdate(I index, A value, S source) {
        return set(index, Maybe.ofNullable(value), source);
    }

    default S remove(I index, S source) {
        return set(index, Maybe.none(), source);
    }

    default S modify(I index, Function<? super A, ? extends A> f, S source) {
        return at(index).modify(value -> value.map(f), source);
    }

    default boolean contains(I index, S source) {
        return get(index, source).isDefined();
    }

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

package com.flechazo.optics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@FunctionalInterface
public interface AffineRebuilder<S, A, T> extends BiFunction<S, A, T>, Serializable {
    static <K, V> Map<K, V> mapValue(Map<K, V> source, K key, V value) {
        if (!source.containsKey(key)) {
            return source;
        }
        LinkedHashMap<K, V> result = new LinkedHashMap<>(source);
        result.put(key, value);
        return Collections.unmodifiableMap(result);
    }

    static <A> List<A> listIndex(List<A> source, int index, A value) {
        if (index < 0 || index >= source.size()) {
            return source;
        }
        ArrayList<A> result = new ArrayList<>(source);
        result.set(index, value);
        return Collections.unmodifiableList(result);
    }
}

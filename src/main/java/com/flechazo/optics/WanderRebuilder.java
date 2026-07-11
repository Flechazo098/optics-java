package com.flechazo.optics;

import java.io.Serializable;
import com.flechazo.hkt.Tuple2;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@FunctionalInterface
public interface WanderRebuilder<S, A> extends BiFunction<S, List<A>, S>, Serializable {
    static <A> List<A> list(List<A> source, List<A> values) {
        return List.copyOf(values);
    }

    static <A> Set<A> set(Set<A> source, List<A> values) {
        return new LinkedHashSet<>(values);
    }

    static <K, V> Map<K, V> mapValues(Map<K, V> source, List<V> values) {
        if (source.size() != values.size()) {
            throw new IllegalArgumentException("Map value count changed during traversal rebuild");
        }
        LinkedHashMap<K, V> result = new LinkedHashMap<>(source.size());
        int index = 0;
        for (K key : source.keySet()) {
            result.put(key, values.get(index++));
        }
        return result;
    }

    static <K, V> Map<K, V> mapEntries(Map<K, V> source, List<Tuple2<K, V>> values) {
        LinkedHashMap<K, V> result = new LinkedHashMap<>(values.size());
        for (Tuple2<K, V> value : values) {
            result.put(value.first(), value.second());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static <A> A[] array(A[] source, List<A> values) {
        A[] result = (A[]) Array.newInstance(source.getClass().getComponentType(), values.size());
        return values.toArray(result);
    }

    static String stringCharacters(String source, List<Character> values) {
        StringBuilder result = new StringBuilder(values.size());
        values.forEach(result::append);
        return result.toString();
    }
}

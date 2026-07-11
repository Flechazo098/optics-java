package com.flechazo.optics;

import java.io.Serializable;
import com.flechazo.hkt.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@FunctionalInterface
public interface WanderGetter<S, A> extends Function<S, Iterable<? extends A>>, Serializable {
    static <A> List<A> list(List<A> source) {
        return source;
    }

    static <A> List<A> set(Set<A> source) {
        return List.copyOf(source);
    }

    static <K, V> List<V> mapValues(Map<K, V> source) {
        return List.copyOf(source.values());
    }

    static <K, V> List<Tuple2<K, V>> mapEntries(Map<K, V> source) {
        ArrayList<Tuple2<K, V>> result = new ArrayList<>(source.size());
        source.forEach((key, value) -> result.add(Tuple2.of(key, value)));
        return List.copyOf(result);
    }

    static <A> List<A> array(A[] source) {
        return Arrays.asList(source);
    }

    static List<Character> stringCharacters(String source) {
        ArrayList<Character> result = new ArrayList<>(source.length());
        for (int index = 0; index < source.length(); index++) {
            result.add(source.charAt(index));
        }
        return List.copyOf(result);
    }
}

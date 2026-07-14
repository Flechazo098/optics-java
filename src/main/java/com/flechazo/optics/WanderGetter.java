package com.flechazo.optics;

import java.io.Serializable;
import com.flechazo.hkt.tuple.Tuple2;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

@FunctionalInterface
public interface WanderGetter<S, A> extends Function<S, Iterable<? extends A>>, Serializable {
    static <A> List<A> list(List<A> source) {
        return source;
    }

    static <A> Iterable<A> set(Set<A> source) {
        return source;
    }

    static <K, V> Iterable<V> mapValues(Map<K, V> source) {
        return source.values();
    }

    static <K, V> Iterable<Tuple2<K, V>> mapEntries(Map<K, V> source) {
        return () -> new Iterator<>() {
            private final Iterator<Map.Entry<K, V>> entries = source.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return entries.hasNext();
            }

            @Override
            public Tuple2<K, V> next() {
                Map.Entry<K, V> entry = entries.next();
                return Tuple2.of(entry.getKey(), entry.getValue());
            }
        };
    }

    static <A> Iterable<A> array(A[] source) {
        return () -> new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < source.length;
            }

            @Override
            public A next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return source[index++];
            }
        };
    }

    static Iterable<Character> stringCharacters(String source) {
        return () -> new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < source.length();
            }

            @Override
            public Character next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return source.charAt(index++);
            }
        };
    }
}

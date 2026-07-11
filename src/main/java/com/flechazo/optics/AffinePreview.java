package com.flechazo.optics;

import com.flechazo.hkt.Either;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface AffinePreview<S, T, A> extends Function<S, Either<T, A>>, Serializable {
    static <K, V> Either<Map<K, V>, V> mapValue(Map<K, V> source, K key) {
        return source.containsKey(key) ? Either.right(source.get(key)) : Either.left(source);
    }

    static <A> Either<List<A>, A> listIndex(List<A> source, int index) {
        return index >= 0 && index < source.size() ? Either.right(source.get(index)) : Either.left(source);
    }
}

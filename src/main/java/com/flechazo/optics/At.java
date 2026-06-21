package com.flechazo.optics;

import com.flechazo.hkt.Maybe;

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
}

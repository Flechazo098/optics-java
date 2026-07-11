package com.flechazo.optics.indexed;

import com.flechazo.hkt.Monoid;

import java.util.function.BiFunction;

public interface IndexedGetter<I, S, A> extends IndexedFold<I, S, A> {
    A get(S source);

    I index();

    @Override
    default <M> M ifoldMap(
            Monoid<M> monoid,
            BiFunction<? super I, ? super A, ? extends M> f,
            S source) {
        return f.apply(index(), get(source));
    }
}

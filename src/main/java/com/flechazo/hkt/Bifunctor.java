package com.flechazo.hkt;

import java.util.function.Function;

public interface Bifunctor<F extends K2> {
    <A, B, C, D> App2<F, C, D> bimap(
            Function<? super C, ? extends A> left,
            Function<? super B, ? extends D> right,
            App2<F, A, B> value);

    default <A, B, C> App2<F, C, B> mapFirst(
            Function<? super C, ? extends A> f,
            App2<F, A, B> value) {
        return bimap(f, Function.identity(), value);
    }

    default <A, B, D> App2<F, A, D> mapSecond(
            Function<? super B, ? extends D> f,
            App2<F, A, B> value) {
        return bimap(Function.identity(), f, value);
    }
}

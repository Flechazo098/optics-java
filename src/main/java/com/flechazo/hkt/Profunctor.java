package com.flechazo.hkt;

import java.util.function.Function;

public interface Profunctor<P extends K2> {
    <A, B, C, D> App2<P, C, D> dimap(
            Function<? super C, ? extends A> left,
            Function<? super B, ? extends D> right,
            App2<P, A, B> value);

    default <A, B, C> App2<P, C, B> lmap(
            Function<? super C, ? extends A> f,
            App2<P, A, B> value) {
        return dimap(f, Function.identity(), value);
    }

    default <A, B, D> App2<P, A, D> rmap(
            Function<? super B, ? extends D> f,
            App2<P, A, B> value) {
        return dimap(Function.identity(), f, value);
    }
}

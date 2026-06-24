package com.flechazo.hkt;

public interface Wander<S, T, A, B> {
    <F extends K1> FunctionArrow<S, App<F, T>> wander(
            Applicative<F, ?> applicative,
            FunctionArrow<A, App<F, B>> input);
}

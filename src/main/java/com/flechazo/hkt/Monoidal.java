package com.flechazo.hkt;

public interface Monoidal<P extends K2> extends Profunctor<P> {
    <A, B, C, D> App2<P, Pair<A, C>, Pair<B, D>> par(
            App2<P, A, B> left,
            App2<P, C, D> right);
}

package com.flechazo.hkt;

public interface Cartesian<P extends K2> extends Profunctor<P> {
    <A, B, C> App2<P, Pair<A, C>, Pair<B, C>> first(App2<P, A, B> value);

    <A, B, C> App2<P, Pair<C, A>, Pair<C, B>> second(App2<P, A, B> value);
}

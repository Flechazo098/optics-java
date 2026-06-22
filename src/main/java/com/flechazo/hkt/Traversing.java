package com.flechazo.hkt;

public interface Traversing<P extends K2> extends Profunctor<P> {
    <T extends K1, A, B> App2<P, App<T, A>, App<T, B>> traversing(
            Traversable<T> traversable,
            App2<P, A, B> value);
}

package com.flechazo.hkt;

public interface Mapping<P extends K2> extends Profunctor<P> {
    <F extends K1, A, B> App2<P, App<F, A>, App<F, B>> mapping(
            Functor<F> functor,
            App2<P, A, B> value);
}

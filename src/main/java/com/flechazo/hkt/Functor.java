package com.flechazo.hkt;

import java.util.function.Function;

public interface Functor<F extends K1> {
    <A, B> App<F, B> map(Function<? super A, ? extends B> f, App<F, A> fa);
}

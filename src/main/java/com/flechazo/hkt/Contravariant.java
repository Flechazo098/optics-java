package com.flechazo.hkt;

import java.util.function.Function;

public interface Contravariant<F extends K1> {
    <A, B> App<F, B> contramap(Function<? super B, ? extends A> f, App<F, A> value);
}

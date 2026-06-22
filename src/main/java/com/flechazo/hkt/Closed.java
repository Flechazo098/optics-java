package com.flechazo.hkt;

import java.util.function.Function;

public interface Closed<P extends K2> extends Profunctor<P> {
    <A, B, X> App2<P, Function<X, A>, Function<X, B>> closed(App2<P, A, B> value);
}

package com.flechazo.hkt;

import java.util.function.Function;

public interface Monad<F extends K1, Proof extends Applicative.Mu> extends Applicative<F, Proof> {
    interface Mu extends Applicative.Mu {
    }

    <A, B> App<F, B> flatMap(Function<? super A, ? extends App<F, B>> f, App<F, A> fa);

    @Override
    default <A, B> App<F, B> map(Function<? super A, ? extends B> f, App<F, A> fa) {
        return flatMap(value -> of(f.apply(value)), fa);
    }

    @Override
    default <A, B> App<F, B> ap(App<F, ? extends Function<A, B>> ff, App<F, A> fa) {
        return flatMap(function -> map(function, fa), ff);
    }
}

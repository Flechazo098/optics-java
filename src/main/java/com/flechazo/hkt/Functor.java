package com.flechazo.hkt;

import java.util.function.Function;

public interface Functor<F extends K1, Proof extends Functor.Mu> extends Kind1<F, Proof> {
    interface Mu extends Kind1.Mu {
    }

    static <F extends K1, Proof extends Mu> Functor<F, Proof> unbox(App<Proof, F> proofBox) {
        return (Functor<F, Proof>) proofBox;
    }

    <A, B> App<F, B> map(Function<? super A, ? extends B> f, App<F, A> fa);
}

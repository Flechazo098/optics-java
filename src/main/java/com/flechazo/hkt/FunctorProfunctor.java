package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

public interface FunctorProfunctor<T extends K1, P extends K2, Proof extends FunctorProfunctor.Mu<T>>
        extends Kind2<P, Proof> {
    interface Mu<T extends K1> extends Kind2.Mu {
    }

    static <T extends K1, P extends K2, Proof extends Mu<T>> FunctorProfunctor<T, P, Proof> unbox(
            App<Proof, P> proofBox) {
        return (FunctorProfunctor<T, P, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, FunctorProfunctor.class);
    }

    <A, B, F extends K1> App2<P, App<F, A>, App<F, B>> distribute(
            App<? extends T, F> proof,
            App2<P, A, B> input);
}

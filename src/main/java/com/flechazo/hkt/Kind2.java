package com.flechazo.hkt;

public interface Kind2<F extends K2, Proof extends Kind2.Mu> extends App<Proof, F> {

    static <F extends K2, Proof extends Mu> Kind2<F, Proof> unbox(App<Proof, F> proofBox) {
        return (Kind2<F, Proof>) proofBox;
    }

    interface Mu extends K1 {
    }
}

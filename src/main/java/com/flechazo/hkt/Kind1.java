package com.flechazo.hkt;

public interface Kind1<F extends K1, Proof extends Kind1.Mu> extends App<Proof, F> {

    static <F extends K1, Proof extends Mu> Kind1<F, Proof> unbox(App<Proof, F> proofBox) {
        return (Kind1<F, Proof>) proofBox;
    }

    interface Mu extends K1 {
    }
}

package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

public interface Kind1<F extends K1, Proof extends Kind1.Mu> extends App<Proof, F> {

    static <F extends K1, Proof extends Mu> Kind1<F, Proof> unbox(App<Proof, F> proofBox) {
        return (Kind1<F, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, Kind1.class);
    }

    interface Mu extends K1 {
    }
}

package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import com.google.common.reflect.TypeToken;

public interface Traversing<P extends K2, Proof extends Traversing.Mu> extends AffineP<P, Proof> {
    interface Mu extends AffineP.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> Traversing<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Traversing<P, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, Traversing.class);
    }

    <S, T, A, B> App2<P, S, T> wander(
            Wander<S, T, A, B> wander,
            App2<P, A, B> input);
}

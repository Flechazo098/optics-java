package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import com.google.common.reflect.TypeToken;

public interface Choice<P extends K2, Proof extends Choice.Mu> extends Cocartesian<P, Proof> {
    interface Mu extends Cocartesian.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> Choice<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Choice<P, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, Choice.class);
    }
}

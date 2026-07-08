package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import com.google.common.reflect.TypeToken;

public interface Mapping<P extends K2, Proof extends Mapping.Mu> extends Traversing<P, Proof> {
    interface Mu extends Traversing.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> Mapping<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Mapping<P, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, Mapping.class);
    }

    <F extends K1, A, B> App2<P, App<F, A>, App<F, B>> mapping(
            Functor<F, ?> functor,
            App2<P, A, B> value);
}

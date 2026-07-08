package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import com.google.common.reflect.TypeToken;

public interface ReCocartesian<P extends K2, Proof extends ReCocartesian.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> ReCocartesian<P, Proof> unbox(App<Proof, P> proofBox) {
        return (ReCocartesian<P, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, ReCocartesian.class);
    }

    <A, B, C> App2<P, A, B> unleft(App2<P, Either<A, C>, Either<B, C>> input);

    <A, B, C> App2<P, A, B> unright(App2<P, Either<C, A>, Either<C, B>> input);
}

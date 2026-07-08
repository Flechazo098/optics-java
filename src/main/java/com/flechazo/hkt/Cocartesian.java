package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import com.google.common.reflect.TypeToken;

public interface Cocartesian<P extends K2, Proof extends Cocartesian.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> Cocartesian<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Cocartesian<P, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, Cocartesian.class);
    }

    <A, B, C> App2<P, Either<A, C>, Either<B, C>> left(App2<P, A, B> value);

    default <A, B, C> App2<P, Either<C, A>, Either<C, B>> right(App2<P, A, B> value) {
        return dimap(left(value), Either::swap, Either::swap);
    }
}

package com.flechazo.optics.util;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.PAffine;
import com.flechazo.optics.Affine;

public final class Affines {
    private Affines() {
    }

    public static <A> Affine<Maybe<A>, A> maybeValue() {
        return Affine.from(Affines.<A, A>pMaybeValue());
    }

    public static <A, B> PAffine<Maybe<A>, Maybe<B>, A, B> pMaybeValue() {
        return PAffine.of(
                value -> value.isDefined()
                        ? Either.right(value.get())
                        : Either.left(Maybe.none()),
                (source, next) -> Maybe.some(next));
    }

}

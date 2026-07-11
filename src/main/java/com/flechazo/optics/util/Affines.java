package com.flechazo.optics.util;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.PAffine;

public final class Affines {
    private Affines() {
    }

    public static <A> PAffine<Maybe<A>, Maybe<A>, A, A> maybeValue() {
        return PAffine.of(
                value -> value.isDefined() ? Either.right(value.get()) : Either.left(Maybe.none()),
                (source, next) -> Maybe.some(next));
    }

}

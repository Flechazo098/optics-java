package com.flechazo.optics.generated;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Prism;

public abstract class GeneratedPrism<S, A> implements Prism<S, S, A, A> {
    @Override
    public final Either<S, A> match(S source) {
        Maybe<A> value = getMaybe(source);
        return value.isDefined() ? Either.right(value.get()) : Either.left(source);
    }
}

package com.flechazo.optics.util;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Affine;
import com.flechazo.optics.PAffine;

/**
 * Provides affine optics for standard optional-value types.
 */
public final class Affines {
    private Affines() {
    }

    /**
     * Creates an affine that focuses the value of a defined {@link Maybe}.
     *
     * @param <A> the focused value type
     * @return an affine with no focus for an empty value
     */
    public static <A> Affine<Maybe<A>, A> maybeValue() {
        return Affine.from(Affines.pMaybeValue());
    }

    /**
     * Creates a polymorphic affine that focuses the value of a defined {@link Maybe}.
     *
     * @param <A> the source value type
     * @param <B> the replacement value type
     * @return an affine that preserves absence and wraps replacements in a defined value
     */
    public static <A, B> PAffine<Maybe<A>, Maybe<B>, A, B> pMaybeValue() {
        return PAffine.of(
                value -> value.isDefined()
                        ? Either.right(value.get())
                        : Either.left(Maybe.none()),
                (source, next) -> Maybe.some(next));
    }

}

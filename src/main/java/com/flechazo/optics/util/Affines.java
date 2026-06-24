package com.flechazo.optics.util;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Affine;

import java.util.Optional;
import java.util.function.Function;

public final class Affines {
    private Affines() {
    }

    public static <A> Affine<Maybe<A>, Maybe<A>, A, A> maybeValue() {
        return Affine.of(
                value -> value.isDefined() ? Either.right(value.get()) : Either.left(Maybe.none()),
                (source, next) -> Maybe.some(next));
    }

    public static <A> Affine<Optional<A>, Optional<A>, A, A> optionalValue() {
        return Affine.of(
                value -> value.isPresent() ? Either.right(value.get()) : Either.left(Optional.empty()),
                (source, next) -> Optional.of(next));
    }

    public static <S, A> Optional<A> getOptional(Affine<S, S, A, A> affine, S source) {
        return Optionals.fromMaybe(affine.getMaybe(source));
    }

    public static <S, A> Optional<A> previewOptional(Affine<S, S, A, A> affine, S source) {
        return getOptional(affine, source);
    }

    public static <S, A, B> Optional<B> mapOptional(
            Affine<S, S, A, A> affine, Function<? super A, ? extends B> f, S source) {
        return Optionals.fromMaybe(affine.getMaybe(source).map(f));
    }
}

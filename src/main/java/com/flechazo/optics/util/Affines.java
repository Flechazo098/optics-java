package com.flechazo.optics.util;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Affine;

import java.util.Optional;
import java.util.function.Function;

public final class Affines {
    private Affines() {
    }

    public static <A> Affine<Maybe<A>, A> maybeValue() {
        return Affine.of(value -> value, (source, next) -> Maybe.some(next), ignored -> Maybe.none());
    }

    public static <A> Affine<Optional<A>, A> optionalValue() {
        return Affine.of(Optionals::toMaybe, (source, next) -> Optional.of(next), ignored -> Optional.empty());
    }

    public static <S, A> Optional<A> getOptional(Affine<S, A> affine, S source) {
        return Optionals.fromMaybe(affine.getMaybe(source));
    }

    public static <S, A> Optional<A> previewOptional(Affine<S, A> affine, S source) {
        return getOptional(affine, source);
    }

    public static <S, A, B> Optional<B> mapOptional(
            Affine<S, A> affine, Function<? super A, ? extends B> f, S source) {
        return Optionals.fromMaybe(affine.mapMaybe(f, source));
    }
}

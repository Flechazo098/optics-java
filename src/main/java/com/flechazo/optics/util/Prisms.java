package com.flechazo.optics.util;

import com.flechazo.hkt.*;
import com.flechazo.optics.Prism;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Prisms {
    private Prisms() {
    }

    public static <A> Prism<Maybe<A>, A> some() {
        return Prism.of(value -> value, Maybe::some);
    }

    public static <A> Prism<Optional<A>, A> optionalSome() {
        return Prism.of(Optionals::toMaybe, Optional::of);
    }

    public static <A> Prism<Maybe<A>, Unit> none() {
        return Prism.of(value -> value.isEmpty() ? Maybe.some(Unit.INSTANCE) : Maybe.none(), ignored -> Maybe.none());
    }

    public static <S, A> Optional<A> getOptional(Prism<S, A> prism, S source) {
        return Optionals.fromMaybe(prism.getMaybe(source));
    }

    public static <S, A> Optional<A> previewOptional(Prism<S, A> prism, S source) {
        return Optionals.fromMaybe(prism.getMaybe(source));
    }

    public static <S, A, B> Optional<B> mapOptional(
            Prism<S, A> prism, Function<? super A, ? extends B> f, S source) {
        return Optionals.fromMaybe(prism.mapMaybe(f, source));
    }

    public static <L, R> Prism<Either<L, R>, L> left() {
        return Prism.of(value -> value.isLeft() ? Maybe.some(value.left()) : Maybe.none(), Either::left);
    }

    public static <L, R> Prism<Either<L, R>, R> right() {
        return Prism.of(value -> value.isRight() ? Maybe.some(value.right()) : Maybe.none(), Either::right);
    }

    public static <A> Prism<Try<A>, A> success() {
        return Prism.of(value -> value.isSuccess() ? Maybe.some(value.get()) : Maybe.none(), Try::success);
    }

    public static <A> Prism<Try<A>, Throwable> failure() {
        return Prism.of(value -> value.isFailure() ? Maybe.some(value.cause()) : Maybe.none(), Try::failure);
    }

    public static <E, A> Prism<Validated<E, A>, A> valid() {
        return Prism.of(value -> value.isValid() ? Maybe.some(value.value()) : Maybe.none(), Validated::valid);
    }

    public static <E, A> Prism<Validated<E, A>, E> invalid() {
        return Prism.of(value -> value.isInvalid() ? Maybe.some(value.error()) : Maybe.none(), Validated::invalid);
    }

    public static <S, A extends S> Prism<S, A> instanceOf(Class<A> subtype) {
        Objects.requireNonNull(subtype, "subtype");
        return Prism.of(
                source -> subtype.isInstance(source) ? Maybe.some(subtype.cast(source)) : Maybe.none(),
                value -> value);
    }

    public static <A> Prism<A, A> only(A expected) {
        return Prism.of(
                value -> Objects.equals(value, expected) ? Maybe.some(value) : Maybe.none(),
                value -> value);
    }

    public static <A> Prism<A, A> matching(Predicate<? super A> predicate) {
        return Prism.of(value -> predicate.test(value) ? Maybe.some(value) : Maybe.none(), value -> value);
    }
}

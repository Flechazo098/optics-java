package com.flechazo.optics.util;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.Prism;
import com.google.common.reflect.TypeToken;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Prisms {
    private Prisms() {
    }

    public static <A> Prism<Maybe<A>, Maybe<A>, A, A> some() {
        return Prism.of(value -> value.isDefined() ? Either.right(value.get()) : Either.left(Maybe.none()), Maybe::some);
    }

    public static <A> Prism<Optional<A>, Optional<A>, A, A> optionalSome() {
        return Prism.of(value -> value.isPresent() ? Either.right(value.get()) : Either.left(Optional.empty()), Optional::of);
    }

    public static <A> Prism<Maybe<A>, Maybe<A>, Unit, Unit> none() {
        return Prism.of(value -> value.isEmpty() ? Either.right(Unit.INSTANCE) : Either.left(value), ignored -> Maybe.none());
    }

    public static <S, A> Optional<A> getOptional(Prism<S, S, A, A> prism, S source) {
        return Optionals.fromMaybe(prism.getMaybe(source));
    }

    public static <S, A> Optional<A> previewOptional(Prism<S, S, A, A> prism, S source) {
        return Optionals.fromMaybe(prism.getMaybe(source));
    }

    public static <S, A, B> Optional<B> mapOptional(
            Prism<S, S, A, A> prism, Function<? super A, ? extends B> f, S source) {
        return Optionals.fromMaybe(prism.getMaybe(source).map(f));
    }

    public static <L, R> Prism<Either<L, R>, Either<L, R>, L, L> left() {
        return Prism.of(value -> value.isLeft() ? Either.right(value.left()) : Either.left(value), Either::left);
    }

    public static <L, R> Prism<Either<L, R>, Either<L, R>, L, L> left(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return left(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> Prism<Either<L, R>, Either<L, R>, L, L> left(
            Type<L> leftType,
            Type<R> rightType) {
        return Prisms.<L, R>left().withTypedOptic(Maybe.some(PointFreeOptic.left(leftType, rightType)));
    }

    public static <L, R> Prism<Either<L, R>, Either<L, R>, R, R> right() {
        return Prism.of(value -> value.isRight() ? Either.right(value.right()) : Either.left(value), Either::right);
    }

    public static <L, R> Prism<Either<L, R>, Either<L, R>, R, R> right(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return right(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> Prism<Either<L, R>, Either<L, R>, R, R> right(
            Type<L> leftType,
            Type<R> rightType) {
        return Prisms.<L, R>right().withTypedOptic(Maybe.some(PointFreeOptic.right(leftType, rightType)));
    }

    public static <A> Prism<Try<A>, Try<A>, A, A> success() {
        return Prism.of(value -> value.isSuccess() ? Either.right(value.get()) : Either.left(value), Try::success);
    }

    public static <A> Prism<Try<A>, Try<A>, Throwable, Throwable> failure() {
        return Prism.of(value -> value.isFailure() ? Either.right(value.cause()) : Either.left(value), Try::failure);
    }

    public static <E, A> Prism<Validated<E, A>, Validated<E, A>, A, A> valid() {
        return Prism.of(value -> value.isValid() ? Either.right(value.value()) : Either.left(value), Validated::valid);
    }

    public static <E, A> Prism<Validated<E, A>, Validated<E, A>, A, A> valid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return valid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> Prism<Validated<E, A>, Validated<E, A>, A, A> valid(
            Type<E> errorType,
            Type<A> valueType) {
        Prism<Validated<E, A>, Validated<E, A>, A, A> prism = Prisms.valid();
        return prism.withTypedOptic(Maybe.some(PointFreeOptic.prism(
                "validatedValid",
                prism,
                Types.validated(errorType, valueType),
                valueType)));
    }

    public static <E, A> Prism<Validated<E, A>, Validated<E, A>, E, E> invalid() {
        return Prism.of(value -> value.isInvalid() ? Either.right(value.error()) : Either.left(value), Validated::invalid);
    }

    public static <E, A> Prism<Validated<E, A>, Validated<E, A>, E, E> invalid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return invalid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> Prism<Validated<E, A>, Validated<E, A>, E, E> invalid(
            Type<E> errorType,
            Type<A> valueType) {
        Prism<Validated<E, A>, Validated<E, A>, E, E> prism = Prisms.invalid();
        return prism.withTypedOptic(Maybe.some(PointFreeOptic.prism(
                "validatedInvalid",
                prism,
                Types.validated(errorType, valueType),
                errorType)));
    }

    public static <S, A extends S> Prism<S, S, A, A> instanceOf(Class<A> subtype) {
        Objects.requireNonNull(subtype, "subtype");
        return Prism.of(
                source -> subtype.isInstance(source) ? Either.right(subtype.cast(source)) : Either.left(source),
                value -> value);
    }

    public static <A> Prism<A, A, A, A> only(A expected) {
        return Prism.of(
                value -> Objects.equals(value, expected) ? Either.right(value) : Either.left(value),
                value -> value);
    }

    public static <A> Prism<A, A, A, A> matching(Predicate<? super A> predicate) {
        return Prism.of(value -> predicate.test(value) ? Either.right(value) : Either.left(value), value -> value);
    }
}

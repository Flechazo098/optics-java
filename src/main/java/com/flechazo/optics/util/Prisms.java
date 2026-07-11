package com.flechazo.optics.util;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PPrism;
import com.flechazo.optics.internal.OpticMetadata;
import com.google.common.reflect.TypeToken;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Prisms {
    private Prisms() {
    }

    public static <A> PPrism<Maybe<A>, Maybe<A>, A, A> some() {
        return PPrism.of(value -> value.isDefined() ? Either.right(value.get()) : Either.left(Maybe.none()), Maybe::some);
    }

    public static <A> PPrism<Maybe<A>, Maybe<A>, Unit, Unit> none() {
        return PPrism.of(value -> value.isEmpty() ? Either.right(Unit.INSTANCE) : Either.left(value), ignored -> Maybe.none());
    }

    public static <L, R> PPrism<Either<L, R>, Either<L, R>, L, L> left() {
        return PPrism.of(value -> value.isLeft() ? Either.right(value.left()) : Either.left(value), Either::left);
    }

    public static <L, R> PPrism<Either<L, R>, Either<L, R>, L, L> left(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return left(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> PPrism<Either<L, R>, Either<L, R>, L, L> left(
            Type<L> leftType,
            Type<R> rightType) {
        return OpticMetadata.optic(
                Prisms.<L, R>left(), Maybe.some(PointFreeOptic.left(leftType, rightType)));
    }

    public static <L, R> PPrism<Either<L, R>, Either<L, R>, R, R> right() {
        return PPrism.of(value -> value.isRight() ? Either.right(value.right()) : Either.left(value), Either::right);
    }

    public static <L, R> PPrism<Either<L, R>, Either<L, R>, R, R> right(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return right(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> PPrism<Either<L, R>, Either<L, R>, R, R> right(
            Type<L> leftType,
            Type<R> rightType) {
        return OpticMetadata.optic(
                Prisms.<L, R>right(), Maybe.some(PointFreeOptic.right(leftType, rightType)));
    }

    public static <A> PPrism<Try<A>, Try<A>, A, A> success() {
        return PPrism.of(value -> value.isSuccess() ? Either.right(value.get()) : Either.left(value), Try::success);
    }

    public static <A> PPrism<Try<A>, Try<A>, Throwable, Throwable> failure() {
        return PPrism.of(value -> value.isFailure() ? Either.right(value.cause()) : Either.left(value), Try::failure);
    }

    public static <E, A> PPrism<Validated<E, A>, Validated<E, A>, A, A> valid() {
        return PPrism.of(value -> value.isValid() ? Either.right(value.value()) : Either.left(value), Validated::valid);
    }

    public static <E, A> PPrism<Validated<E, A>, Validated<E, A>, A, A> valid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return valid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> PPrism<Validated<E, A>, Validated<E, A>, A, A> valid(
            Type<E> errorType,
            Type<A> valueType) {
        PPrism<Validated<E, A>, Validated<E, A>, A, A> prism = Prisms.valid();
        return OpticMetadata.optic(prism, Maybe.some(PointFreeOptic.prism(
                "validatedValid",
                prism,
                Types.validated(errorType, valueType),
                valueType)));
    }

    public static <E, A> PPrism<Validated<E, A>, Validated<E, A>, E, E> invalid() {
        return PPrism.of(value -> value.isInvalid() ? Either.right(value.error()) : Either.left(value), Validated::invalid);
    }

    public static <E, A> PPrism<Validated<E, A>, Validated<E, A>, E, E> invalid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return invalid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> PPrism<Validated<E, A>, Validated<E, A>, E, E> invalid(
            Type<E> errorType,
            Type<A> valueType) {
        PPrism<Validated<E, A>, Validated<E, A>, E, E> prism = Prisms.invalid();
        return OpticMetadata.optic(prism, Maybe.some(PointFreeOptic.prism(
                "validatedInvalid",
                prism,
                Types.validated(errorType, valueType),
                errorType)));
    }

    public static <S, A extends S> PPrism<S, S, A, A> instanceOf(Class<A> subtype) {
        Objects.requireNonNull(subtype, "subtype");
        return PPrism.of(
                source -> subtype.isInstance(source) ? Either.right(subtype.cast(source)) : Either.left(source),
                value -> value);
    }

    public static <A> PPrism<A, A, A, A> only(A expected) {
        return PPrism.of(
                value -> Objects.equals(value, expected) ? Either.right(value) : Either.left(value),
                value -> value);
    }

    public static <A> PPrism<A, A, A, A> matching(Predicate<? super A> predicate) {
        return PPrism.of(value -> predicate.test(value) ? Either.right(value) : Either.left(value), value -> value);
    }
}

package com.flechazo.optics.util;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PPrism;
import com.flechazo.optics.Prism;
import com.flechazo.optics.internal.OpticMetadata;
import com.google.common.reflect.TypeToken;

import java.util.Objects;
import java.util.function.Predicate;

public final class Prisms {
    private Prisms() {
    }

    public static <A> Prism<Maybe<A>, A> some() {
        return Prism.from(Prisms.<A, A>pSome());
    }

    public static <A, B> PPrism<Maybe<A>, Maybe<B>, A, B> pSome() {
        return PPrism.of(
                value -> value.isDefined()
                        ? Either.right(value.get())
                        : Either.left(Maybe.none()),
                Maybe::some);
    }

    public static <A> Prism<Maybe<A>, Unit> none() {
        return Prism.from(PPrism.of(
                value -> value.isEmpty() ? Either.right(Unit.INSTANCE) : Either.left(value),
                ignored -> Maybe.none()));
    }

    public static <L, R> Prism<Either<L, R>, L> left() {
        return Prism.from(Prisms.<L, L, R>pLeft());
    }

    public static <L, M, R> PPrism<Either<L, R>, Either<M, R>, L, M> pLeft() {
        return PPrism.of(
                value -> value.isLeft()
                        ? Either.right(value.left())
                        : Either.left(Either.right(value.right())),
                Either::left);
    }

    public static <L, R> Prism<Either<L, R>, L> left(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return left(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> Prism<Either<L, R>, L> left(
            Type<L> leftType,
            Type<R> rightType) {
        return Prism.from(Prisms.<L, L, R>pLeft(leftType, leftType, rightType));
    }

    public static <L, M, R> PPrism<Either<L, R>, Either<M, R>, L, M> pLeft(
            TypeToken<L> leftType,
            TypeToken<M> targetLeftType,
            TypeToken<R> rightType) {
        return pLeft(
                Types.witness(leftType), Types.witness(targetLeftType), Types.witness(rightType));
    }

    public static <L, M, R> PPrism<Either<L, R>, Either<M, R>, L, M> pLeft(
            Type<L> leftType,
            Type<M> targetLeftType,
            Type<R> rightType) {
        return OpticMetadata.optic(
                Prisms.<L, M, R>pLeft(),
                Maybe.some(PointFreeOptic.left(leftType, targetLeftType, rightType)));
    }

    public static <L, R> Prism<Either<L, R>, R> right() {
        return Prism.from(Prisms.<L, R, R>pRight());
    }

    public static <L, R, B> PPrism<Either<L, R>, Either<L, B>, R, B> pRight() {
        return PPrism.of(
                value -> value.isRight()
                        ? Either.right(value.right())
                        : Either.left(Either.left(value.left())),
                Either::right);
    }

    public static <L, R> Prism<Either<L, R>, R> right(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return right(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> Prism<Either<L, R>, R> right(
            Type<L> leftType,
            Type<R> rightType) {
        return Prism.from(Prisms.<L, R, R>pRight(leftType, rightType, rightType));
    }

    public static <L, R, B> PPrism<Either<L, R>, Either<L, B>, R, B> pRight(
            TypeToken<L> leftType,
            TypeToken<R> rightType,
            TypeToken<B> targetRightType) {
        return pRight(
                Types.witness(leftType), Types.witness(rightType), Types.witness(targetRightType));
    }

    public static <L, R, B> PPrism<Either<L, R>, Either<L, B>, R, B> pRight(
            Type<L> leftType,
            Type<R> rightType,
            Type<B> targetRightType) {
        return OpticMetadata.optic(
                Prisms.<L, R, B>pRight(),
                Maybe.some(PointFreeOptic.right(leftType, rightType, targetRightType)));
    }

    public static <A> Prism<Try<A>, A> success() {
        return Prism.from(Prisms.<A, A>pSuccess());
    }

    public static <A, B> PPrism<Try<A>, Try<B>, A, B> pSuccess() {
        return PPrism.of(
                value -> value.isSuccess()
                        ? Either.right(value.get())
                        : Either.left(Try.failure(value.cause())),
                Try::success);
    }

    public static <A> Prism<Try<A>, Throwable> failure() {
        return Prism.from(PPrism.of(
                value -> value.isFailure() ? Either.right(value.cause()) : Either.left(value),
                Try::failure));
    }

    public static <E, A> Prism<Validated<E, A>, A> valid() {
        return Prism.from(Prisms.<E, A, A>pValid());
    }

    public static <E, A, B> PPrism<Validated<E, A>, Validated<E, B>, A, B> pValid() {
        return PPrism.of(
                value -> value.isValid()
                        ? Either.right(value.value())
                        : Either.left(Validated.invalid(value.error())),
                Validated::valid);
    }

    public static <E, A> Prism<Validated<E, A>, A> valid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return valid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> Prism<Validated<E, A>, A> valid(
            Type<E> errorType,
            Type<A> valueType) {
        return Prism.from(Prisms.<E, A, A>pValid(errorType, valueType, valueType));
    }

    public static <E, A, B> PPrism<Validated<E, A>, Validated<E, B>, A, B> pValid(
            TypeToken<E> errorType,
            TypeToken<A> valueType,
            TypeToken<B> targetValueType) {
        return pValid(
                Types.witness(errorType), Types.witness(valueType), Types.witness(targetValueType));
    }

    public static <E, A, B> PPrism<Validated<E, A>, Validated<E, B>, A, B> pValid(
            Type<E> errorType,
            Type<A> valueType,
            Type<B> targetValueType) {
        PPrism<Validated<E, A>, Validated<E, B>, A, B> prism = Prisms.pValid();
        return OpticMetadata.optic(prism, Maybe.some(PointFreeOptic.prism(
                "validatedValid",
                prism,
                Types.validated(errorType, valueType),
                Types.validated(errorType, targetValueType),
                valueType,
                targetValueType)));
    }

    public static <E, A> Prism<Validated<E, A>, E> invalid() {
        return Prism.from(Prisms.<E, E, A>pInvalid());
    }

    public static <E, F, A> PPrism<Validated<E, A>, Validated<F, A>, E, F> pInvalid() {
        return PPrism.of(
                value -> value.isInvalid()
                        ? Either.right(value.error())
                        : Either.left(Validated.valid(value.value())),
                Validated::invalid);
    }

    public static <E, A> Prism<Validated<E, A>, E> invalid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return invalid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> Prism<Validated<E, A>, E> invalid(
            Type<E> errorType,
            Type<A> valueType) {
        return Prism.from(Prisms.<E, E, A>pInvalid(errorType, errorType, valueType));
    }

    public static <E, F, A> PPrism<Validated<E, A>, Validated<F, A>, E, F> pInvalid(
            TypeToken<E> errorType,
            TypeToken<F> targetErrorType,
            TypeToken<A> valueType) {
        return pInvalid(
                Types.witness(errorType), Types.witness(targetErrorType), Types.witness(valueType));
    }

    public static <E, F, A> PPrism<Validated<E, A>, Validated<F, A>, E, F> pInvalid(
            Type<E> errorType,
            Type<F> targetErrorType,
            Type<A> valueType) {
        PPrism<Validated<E, A>, Validated<F, A>, E, F> prism = Prisms.pInvalid();
        return OpticMetadata.optic(prism, Maybe.some(PointFreeOptic.prism(
                "validatedInvalid",
                prism,
                Types.validated(errorType, valueType),
                Types.validated(targetErrorType, valueType),
                errorType,
                targetErrorType)));
    }

    public static <S, A extends S> Prism<S, A> instanceOf(Class<A> subtype) {
        Objects.requireNonNull(subtype, "subtype");
        return Prism.from(PPrism.of(
                source -> subtype.isInstance(source) ? Either.right(subtype.cast(source)) : Either.left(source),
                value -> value));
    }

    public static <A> Prism<A, A> only(A expected) {
        return Prism.from(PPrism.of(
                value -> Objects.equals(value, expected) ? Either.right(value) : Either.left(value),
                value -> value));
    }

    public static <A> Prism<A, A> matching(Predicate<? super A> predicate) {
        return Prism.from(PPrism.of(
                value -> predicate.test(value) ? Either.right(value) : Either.left(value),
                value -> value));
    }
}

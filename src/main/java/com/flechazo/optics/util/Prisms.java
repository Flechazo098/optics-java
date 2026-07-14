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

/**
 * Provides prisms for common sum and value types.
 */
public final class Prisms {
    private Prisms() {
    }

    /**
     * Creates a prism that focuses the value of a defined {@link Maybe}.
     *
     * @param <A> the focused value type
     * @return a prism that rejects an empty value
     */
    public static <A> Prism<Maybe<A>, A> some() {
        return Prism.from(Prisms.pSome());
    }

    /**
     * Creates a polymorphic prism that focuses the value of a defined {@link Maybe}.
     *
     * @param <A> the source value type
     * @param <B> the replacement value type
     * @return a prism that rejects absence and wraps replacements in a defined value
     */
    public static <A, B> PPrism<Maybe<A>, Maybe<B>, A, B> pSome() {
        return PPrism.of(
                value -> value.isDefined()
                        ? Either.right(value.get())
                        : Either.left(Maybe.none()),
                Maybe::some);
    }

    /**
     * Creates a prism that selects an empty {@link Maybe}.
     *
     * @param <A> the value type
     * @return a prism focused on {@link Unit} when the source is empty
     */
    public static <A> Prism<Maybe<A>, Unit> none() {
        return Prism.from(PPrism.of(
                value -> value.isEmpty() ? Either.right(Unit.INSTANCE) : Either.left(value),
                ignored -> Maybe.none()));
    }

    /**
     * Creates a prism that focuses the left alternative of an {@link Either}.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @return a prism that rejects right values
     */
    public static <L, R> Prism<Either<L, R>, L> left() {
        return Prism.from(Prisms.pLeft());
    }

    /**
     * Creates a polymorphic prism that focuses the left alternative of an {@link Either}.
     *
     * @param <L> the source left value type
     * @param <M> the replacement left value type
     * @param <R> the unchanged right value type
     * @return a prism that preserves rejected right values
     */
    public static <L, M, R> PPrism<Either<L, R>, Either<M, R>, L, M> pLeft() {
        return PPrism.of(
                value -> value.isLeft()
                        ? Either.right(value.left())
                        : Either.left(Either.right(value.right())),
                Either::left);
    }

    /**
     * Creates a typed prism that focuses the left alternative of an {@link Either}.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param leftType the runtime description of the left value type
     * @param rightType the runtime description of the right value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <L, R> Prism<Either<L, R>, L> left(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return left(Types.witness(leftType), Types.witness(rightType));
    }

    /**
     * Creates a typed prism that focuses the left alternative of an {@link Either}.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param leftType the runtime witness for the left value type
     * @param rightType the runtime witness for the right value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <L, R> Prism<Either<L, R>, L> left(
            Type<L> leftType,
            Type<R> rightType) {
        return Prism.from(Prisms.pLeft(leftType, leftType, rightType));
    }

    /**
     * Creates a typed polymorphic prism that focuses the left alternative of an {@link Either}.
     *
     * @param <L> the source left value type
     * @param <M> the replacement left value type
     * @param <R> the unchanged right value type
     * @param leftType the runtime description of the source left value type
     * @param targetLeftType the runtime description of the replacement left value type
     * @param rightType the runtime description of the right value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <L, M, R> PPrism<Either<L, R>, Either<M, R>, L, M> pLeft(
            TypeToken<L> leftType,
            TypeToken<M> targetLeftType,
            TypeToken<R> rightType) {
        return pLeft(
                Types.witness(leftType), Types.witness(targetLeftType), Types.witness(rightType));
    }

    /**
     * Creates a typed polymorphic prism that focuses the left alternative of an {@link Either}.
     *
     * @param <L> the source left value type
     * @param <M> the replacement left value type
     * @param <R> the unchanged right value type
     * @param leftType the runtime witness for the source left value type
     * @param targetLeftType the runtime witness for the replacement left value type
     * @param rightType the runtime witness for the right value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <L, M, R> PPrism<Either<L, R>, Either<M, R>, L, M> pLeft(
            Type<L> leftType,
            Type<M> targetLeftType,
            Type<R> rightType) {
        return OpticMetadata.optic(
                Prisms.pLeft(),
                Maybe.some(PointFreeOptic.left(leftType, targetLeftType, rightType)));
    }

    /**
     * Creates a prism that focuses the right alternative of an {@link Either}.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @return a prism that rejects left values
     */
    public static <L, R> Prism<Either<L, R>, R> right() {
        return Prism.from(Prisms.pRight());
    }

    /**
     * Creates a polymorphic prism that focuses the right alternative of an {@link Either}.
     *
     * @param <L> the unchanged left value type
     * @param <R> the source right value type
     * @param <B> the replacement right value type
     * @return a prism that preserves rejected left values
     */
    public static <L, R, B> PPrism<Either<L, R>, Either<L, B>, R, B> pRight() {
        return PPrism.of(
                value -> value.isRight()
                        ? Either.right(value.right())
                        : Either.left(Either.left(value.left())),
                Either::right);
    }

    /**
     * Creates a typed prism that focuses the right alternative of an {@link Either}.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param leftType the runtime description of the left value type
     * @param rightType the runtime description of the right value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <L, R> Prism<Either<L, R>, R> right(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return right(Types.witness(leftType), Types.witness(rightType));
    }

    /**
     * Creates a typed prism that focuses the right alternative of an {@link Either}.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param leftType the runtime witness for the left value type
     * @param rightType the runtime witness for the right value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <L, R> Prism<Either<L, R>, R> right(
            Type<L> leftType,
            Type<R> rightType) {
        return Prism.from(Prisms.pRight(leftType, rightType, rightType));
    }

    /**
     * Creates a typed polymorphic prism that focuses the right alternative of an {@link Either}.
     *
     * @param <L> the unchanged left value type
     * @param <R> the source right value type
     * @param <B> the replacement right value type
     * @param leftType the runtime description of the left value type
     * @param rightType the runtime description of the source right value type
     * @param targetRightType the runtime description of the replacement right value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <L, R, B> PPrism<Either<L, R>, Either<L, B>, R, B> pRight(
            TypeToken<L> leftType,
            TypeToken<R> rightType,
            TypeToken<B> targetRightType) {
        return pRight(
                Types.witness(leftType), Types.witness(rightType), Types.witness(targetRightType));
    }

    /**
     * Creates a typed polymorphic prism that focuses the right alternative of an {@link Either}.
     *
     * @param <L> the unchanged left value type
     * @param <R> the source right value type
     * @param <B> the replacement right value type
     * @param leftType the runtime witness for the left value type
     * @param rightType the runtime witness for the source right value type
     * @param targetRightType the runtime witness for the replacement right value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <L, R, B> PPrism<Either<L, R>, Either<L, B>, R, B> pRight(
            Type<L> leftType,
            Type<R> rightType,
            Type<B> targetRightType) {
        return OpticMetadata.optic(
                Prisms.pRight(),
                Maybe.some(PointFreeOptic.right(leftType, rightType, targetRightType)));
    }

    /**
     * Creates a prism that focuses the value of a successful {@link Try}.
     *
     * @param <A> the success value type
     * @return a prism that rejects failed computations
     */
    public static <A> Prism<Try<A>, A> success() {
        return Prism.from(Prisms.pSuccess());
    }

    /**
     * Creates a polymorphic prism that focuses the value of a successful {@link Try}.
     *
     * @param <A> the source success value type
     * @param <B> the replacement success value type
     * @return a prism that preserves rejected failures
     */
    public static <A, B> PPrism<Try<A>, Try<B>, A, B> pSuccess() {
        return PPrism.of(
                value -> value.isSuccess()
                        ? Either.right(value.get())
                        : Either.left(Try.failure(value.cause())),
                Try::success);
    }

    /**
     * Creates a prism that focuses the cause of a failed {@link Try}.
     *
     * @param <A> the success value type
     * @return a prism that rejects successful computations
     */
    public static <A> Prism<Try<A>, Throwable> failure() {
        return Prism.from(PPrism.of(
                value -> value.isFailure() ? Either.right(value.cause()) : Either.left(value),
                Try::failure));
    }

    /**
     * Creates a prism that focuses the value of a valid {@link Validated}.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @return a prism that rejects invalid values
     */
    public static <E, A> Prism<Validated<E, A>, A> valid() {
        return Prism.from(Prisms.pValid());
    }

    /**
     * Creates a polymorphic prism that focuses the value of a valid {@link Validated}.
     *
     * @param <E> the unchanged validation error type
     * @param <A> the source valid value type
     * @param <B> the replacement valid value type
     * @return a prism that preserves rejected invalid values
     */
    public static <E, A, B> PPrism<Validated<E, A>, Validated<E, B>, A, B> pValid() {
        return PPrism.of(
                value -> value.isValid()
                        ? Either.right(value.value())
                        : Either.left(Validated.invalid(value.error())),
                Validated::valid);
    }

    /**
     * Creates a typed prism that focuses the value of a valid {@link Validated}.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param errorType the runtime description of the error type
     * @param valueType the runtime description of the valid value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <E, A> Prism<Validated<E, A>, A> valid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return valid(Types.witness(errorType), Types.witness(valueType));
    }

    /**
     * Creates a typed prism that focuses the value of a valid {@link Validated}.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param errorType the runtime witness for the error type
     * @param valueType the runtime witness for the valid value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <E, A> Prism<Validated<E, A>, A> valid(
            Type<E> errorType,
            Type<A> valueType) {
        return Prism.from(Prisms.pValid(errorType, valueType, valueType));
    }

    /**
     * Creates a typed polymorphic prism that focuses the value of a valid {@link Validated}.
     *
     * @param <E> the unchanged validation error type
     * @param <A> the source valid value type
     * @param <B> the replacement valid value type
     * @param errorType the runtime description of the error type
     * @param valueType the runtime description of the source valid value type
     * @param targetValueType the runtime description of the replacement valid value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <E, A, B> PPrism<Validated<E, A>, Validated<E, B>, A, B> pValid(
            TypeToken<E> errorType,
            TypeToken<A> valueType,
            TypeToken<B> targetValueType) {
        return pValid(
                Types.witness(errorType), Types.witness(valueType), Types.witness(targetValueType));
    }

    /**
     * Creates a typed polymorphic prism that focuses the value of a valid {@link Validated}.
     *
     * @param <E> the unchanged validation error type
     * @param <A> the source valid value type
     * @param <B> the replacement valid value type
     * @param errorType the runtime witness for the error type
     * @param valueType the runtime witness for the source valid value type
     * @param targetValueType the runtime witness for the replacement valid value type
     * @return a prism with type metadata for the supplied alternatives
     */
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

    /**
     * Creates a prism that focuses the error of an invalid {@link Validated}.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @return a prism that rejects valid values
     */
    public static <E, A> Prism<Validated<E, A>, E> invalid() {
        return Prism.from(Prisms.pInvalid());
    }

    /**
     * Creates a polymorphic prism that focuses the error of an invalid {@link Validated}.
     *
     * @param <E> the source validation error type
     * @param <F> the replacement validation error type
     * @param <A> the unchanged valid value type
     * @return a prism that preserves rejected valid values
     */
    public static <E, F, A> PPrism<Validated<E, A>, Validated<F, A>, E, F> pInvalid() {
        return PPrism.of(
                value -> value.isInvalid()
                        ? Either.right(value.error())
                        : Either.left(Validated.valid(value.value())),
                Validated::invalid);
    }

    /**
     * Creates a typed prism that focuses the error of an invalid {@link Validated}.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param errorType the runtime description of the error type
     * @param valueType the runtime description of the valid value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <E, A> Prism<Validated<E, A>, E> invalid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return invalid(Types.witness(errorType), Types.witness(valueType));
    }

    /**
     * Creates a typed prism that focuses the error of an invalid {@link Validated}.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param errorType the runtime witness for the error type
     * @param valueType the runtime witness for the valid value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <E, A> Prism<Validated<E, A>, E> invalid(
            Type<E> errorType,
            Type<A> valueType) {
        return Prism.from(Prisms.pInvalid(errorType, errorType, valueType));
    }

    /**
     * Creates a typed polymorphic prism that focuses the error of an invalid {@link Validated}.
     *
     * @param <E> the source validation error type
     * @param <F> the replacement validation error type
     * @param <A> the unchanged valid value type
     * @param errorType the runtime description of the source error type
     * @param targetErrorType the runtime description of the replacement error type
     * @param valueType the runtime description of the valid value type
     * @return a prism with type metadata for the supplied alternatives
     */
    public static <E, F, A> PPrism<Validated<E, A>, Validated<F, A>, E, F> pInvalid(
            TypeToken<E> errorType,
            TypeToken<F> targetErrorType,
            TypeToken<A> valueType) {
        return pInvalid(
                Types.witness(errorType), Types.witness(targetErrorType), Types.witness(valueType));
    }

    /**
     * Creates a typed polymorphic prism that focuses the error of an invalid {@link Validated}.
     *
     * @param <E> the source validation error type
     * @param <F> the replacement validation error type
     * @param <A> the unchanged valid value type
     * @param errorType the runtime witness for the source error type
     * @param targetErrorType the runtime witness for the replacement error type
     * @param valueType the runtime witness for the valid value type
     * @return a prism with type metadata for the supplied alternatives
     */
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

    /**
     * Creates a prism that selects instances of a subtype.
     *
     * @param <S> the source supertype
     * @param <A> the selected subtype
     * @param subtype the class identifying values accepted by the prism
     * @return a prism that rejects values which are not instances of {@code subtype}
     */
    public static <S, A extends S> Prism<S, A> instanceOf(Class<A> subtype) {
        Objects.requireNonNull(subtype, "subtype");
        return Prism.from(PPrism.of(
                source -> subtype.isInstance(source) ? Either.right(subtype.cast(source)) : Either.left(source),
                value -> value));
    }

    /**
     * Creates a prism that selects values equal to an expected value.
     *
     * @param <A> the value type
     * @param expected the value accepted by the prism
     * @return a prism that rejects unequal values
     */
    public static <A> Prism<A, A> only(A expected) {
        return Prism.from(PPrism.of(
                value -> Objects.equals(value, expected) ? Either.right(value) : Either.left(value),
                value -> value));
    }

    /**
     * Creates a prism that selects values satisfying a predicate.
     *
     * @param <A> the value type
     * @param predicate the condition used to accept values
     * @return a prism that rejects values for which {@code predicate} returns {@code false}
     */
    public static <A> Prism<A, A> matching(Predicate<? super A> predicate) {
        return Prism.from(PPrism.of(
                value -> predicate.test(value) ? Either.right(value) : Either.left(value),
                value -> value));
    }
}

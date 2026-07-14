package com.flechazo.hkt.business.control;

import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.business.data.NonEmptyList;

/**
 * Creates validations that accumulate errors in a non-empty list.
 */
public final class ValidatedNel {
    private ValidatedNel() {
    }

    /**
     * Creates a valid value.
     *
     * @param <E> the error element type
     * @param <A> the valid value type
     * @param value the valid value
     * @return a valid result
     */
    public static <E, A> Validated<NonEmptyList<E>, A> valid(A value) {
        return Validated.valid(value);
    }

    /**
     * Creates an invalid value containing one error.
     *
     * @param <E> the error element type
     * @param <A> the valid value type
     * @param error the validation error
     * @return an invalid result containing {@code error}
     */
    public static <E, A> Validated<NonEmptyList<E>, A> invalid(E error) {
        return Validated.invalid(NonEmptyList.of(error));
    }

    /**
     * Creates an invalid value containing supplied errors.
     *
     * @param <E> the error element type
     * @param <A> the valid value type
     * @param errors the accumulated errors
     * @return an invalid result containing {@code errors}
     */
    public static <E, A> Validated<NonEmptyList<E>, A> invalidAll(NonEmptyList<E> errors) {
        return Validated.invalid(errors);
    }

    /**
     * Returns an applicative that accumulates all validation errors.
     *
     * @param <E> the error element type
     * @return the accumulating validation applicative
     */
    public static <E> Applicative<Validated.RightMu<NonEmptyList<E>>, ?> applicative() {
        return Validated.applicative(NonEmptyList.semigroup());
    }

    /**
     * Returns a selective that accumulates errors from evaluated validation branches.
     *
     * @param <E> the error element type
     * @return the accumulating validation selective
     */
    public static <E> Selective<Validated.RightMu<NonEmptyList<E>>, ?> selective() {
        return Validated.selective(NonEmptyList.semigroup());
    }
}

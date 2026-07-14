package com.flechazo.hkt.business.core;

import com.flechazo.hkt.CheckedSupplier;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;

import java.util.Objects;
import java.util.function.Function;

/**
 * Captures checked computations in explicit success and failure values.
 */
public final class Attempts {
    private Attempts() {
    }

    /**
     * Executes a supplier and captures its outcome in a {@link Try}.
     *
     * @param <A> the result type
     * @param <X> the checked exception type
     * @param supplier the computation to execute
     * @return the successful result or captured failure
     */
    public static <A, X extends Exception> Try<A> tryOf(CheckedSupplier<A, X> supplier) {
        return Try.of(supplier);
    }

    /**
     * Executes a supplier and discards failure details.
     *
     * @param <A> the result type
     * @param <X> the checked exception type
     * @param supplier the computation to execute
     * @return a defined successful result, or an empty value on failure
     */
    public static <A, X extends Exception> Maybe<A> maybe(CheckedSupplier<A, X> supplier) {
        return either(supplier).toMaybe();
    }

    /**
     * Executes a supplier and captures failure in the left alternative.
     *
     * @param <A> the result type
     * @param <X> the checked exception type
     * @param supplier the computation to execute
     * @return a right result or the captured throwable
     */
    public static <A, X extends Exception> Either<Throwable, A> either(CheckedSupplier<A, X> supplier) {
        return Either.catching(supplier);
    }

    /**
     * Executes a supplier and maps failure into an explicit error value.
     *
     * @param <E> the error type
     * @param <A> the result type
     * @param <X> the checked exception type
     * @param supplier the computation to execute
     * @param errorMapper the function converting a failure into an error value
     * @return a right result or the mapped left error
     */
    public static <E, A, X extends Exception> Either<E, A> either(
            CheckedSupplier<A, X> supplier,
            Function<? super Throwable, ? extends E> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper");
        return Either.catching(supplier).mapLeft(error -> Objects.requireNonNull(errorMapper.apply(error), "mapped error"));
    }

    /**
     * Executes a supplier and describes failure with a labeled message.
     *
     * @param <A> the result type
     * @param <X> the checked exception type
     * @param label the context prepended to a failure message
     * @param supplier the computation to execute
     * @return a right result or a labeled left error message
     */
    public static <A, X extends Exception> Either<String, A> either(String label, CheckedSupplier<A, X> supplier) {
        Objects.requireNonNull(label, "label");
        return either(supplier, error -> label + ": " + error.getMessage());
    }
}

package com.flechazo.hkt.business.core;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.CheckedSupplier;
import com.flechazo.hkt.Try;

import java.util.Objects;
import java.util.function.Function;

public final class Attempts {
    private Attempts() {
    }

    public static <A, X extends Exception> Try<A> tryOf(CheckedSupplier<A, X> supplier) {
        return Try.of(supplier);
    }

    public static <A, X extends Exception> Maybe<A> maybe(CheckedSupplier<A, X> supplier) {
        return either(supplier).toMaybe();
    }

    public static <A, X extends Exception> Either<Throwable, A> either(CheckedSupplier<A, X> supplier) {
        return Either.catching(supplier);
    }

    public static <E, A, X extends Exception> Either<E, A> either(
            CheckedSupplier<A, X> supplier,
            Function<? super Throwable, ? extends E> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper");
        return Either.catching(supplier).mapLeft(error -> Objects.requireNonNull(errorMapper.apply(error), "mapped error"));
    }

    public static <A, X extends Exception> Either<String, A> either(String label, CheckedSupplier<A, X> supplier) {
        Objects.requireNonNull(label, "label");
        return either(supplier, error -> label + ": " + error.getMessage());
    }
}
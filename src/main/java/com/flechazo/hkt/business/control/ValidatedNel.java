package com.flechazo.hkt.business.control;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Validated;

public final class ValidatedNel {
    private ValidatedNel() {
    }

    public static <E, A> Validated<NonEmptyList<E>, A> valid(A value) {
        return Validated.valid(value);
    }

    public static <E, A> Validated<NonEmptyList<E>, A> invalid(E error) {
        return Validated.invalid(NonEmptyList.of(error));
    }

    public static <E, A> Validated<NonEmptyList<E>, A> invalidAll(NonEmptyList<E> errors) {
        return Validated.invalid(errors);
    }

    public static <E> Applicative<Validated.RightMu<NonEmptyList<E>>, ?> applicative() {
        return Validated.applicative(NonEmptyList.semigroup());
    }

    public static <E> Selective<Validated.RightMu<NonEmptyList<E>>, ?> selective() {
        return Validated.selective(NonEmptyList.semigroup());
    }
}

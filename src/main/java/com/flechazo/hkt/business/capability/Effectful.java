package com.flechazo.hkt.business.capability;

import com.flechazo.hkt.Try;

import java.util.function.Function;

public interface Effectful<A> extends Chainable<A> {
    A unsafeRun();

    default Try<A> runSafe() {
        return Try.of(this::unsafeRun);
    }

    Effectful<A> handleError(Function<? super Throwable, ? extends A> recovery);

    Effectful<A> handleErrorWith(Function<? super Throwable, ? extends Effectful<A>> recovery);

    Effectful<A> guarantee(Runnable finalizer);
}

package com.flechazo.hkt.business.capability;

import com.flechazo.hkt.Try;

import java.util.function.Function;

/**
 * Represents an executable computation that may fail with a throwable cause.
 *
 * @param <A> the result type
 */
public interface Effectful<A> extends Chainable<A> {
    /**
     * Executes the computation and returns its result.
     *
     * @return the computed result
     */
    A unsafeRun();

    /**
     * Executes the computation and captures a non-fatal failure in a {@link Try}.
     *
     * @return a successful result or the captured failure
     */
    default Try<A> runSafe() {
        return Try.of(this::unsafeRun);
    }

    /**
     * Recovers from a failure by producing a replacement value.
     *
     * @param recovery the function producing a value from the failure
     * @return a computation with the recovery operation attached
     */
    Effectful<A> handleError(Function<? super Throwable, ? extends A> recovery);

    /**
     * Recovers from a failure by selecting another computation.
     *
     * @param recovery the function selecting a replacement computation
     * @return a computation with the recovery operation attached
     */
    Effectful<A> handleErrorWith(Function<? super Throwable, ? extends Effectful<A>> recovery);

    /**
     * Registers an action that runs after completion, whether successful or failed.
     *
     * @param finalizer the completion action
     * @return a computation with the finalizer attached
     */
    Effectful<A> guarantee(Runnable finalizer);
}

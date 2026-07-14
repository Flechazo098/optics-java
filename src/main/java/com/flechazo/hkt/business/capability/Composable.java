package com.flechazo.hkt.business.capability;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a value-producing computation that supports result transformation and observation.
 *
 * @param <A> the result type
 */
public interface Composable<A> {
    /**
     * Transforms the result of this computation.
     *
     * @param <B> the transformed result type
     * @param mapper the result transformation
     * @return a computation producing the transformed result
     */
    <B> Composable<B> map(Function<? super A, ? extends B> mapper);

    /**
     * Observes the result while preserving it.
     *
     * @param consumer the operation invoked with the result
     * @return a computation that produces the original result after observation
     */
    Composable<A> peek(Consumer<? super A> consumer);
}

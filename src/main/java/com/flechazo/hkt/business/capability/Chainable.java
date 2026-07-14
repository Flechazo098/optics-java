package com.flechazo.hkt.business.capability;

import com.flechazo.hkt.business.capability.combinable.Combinable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a computation whose result can determine the next computation.
 *
 * @param <A> the result type
 */
public interface Chainable<A> extends Combinable<A> {
    /**
     * Sequences a computation selected from the current result.
     *
     * @param <B> the next result type
     * @param mapper the function selecting the next computation
     * @return the sequenced computation
     */
    <B> Chainable<B> via(Function<? super A, ? extends Chainable<B>> mapper);

    /**
     * Sequences a computation selected from the current result.
     *
     * @param <B> the next result type
     * @param mapper the function selecting the next computation
     * @return the sequenced computation
     */
    default <B> Chainable<B> flatMap(Function<? super A, ? extends Chainable<B>> mapper) {
        return via(mapper);
    }

    /**
     * Sequences a computation after the current computation and discards the current result.
     *
     * @param <B> the next result type
     * @param supplier the deferred next computation
     * @return the sequenced computation
     */
    <B> Chainable<B> then(Supplier<? extends Chainable<B>> supplier);
}

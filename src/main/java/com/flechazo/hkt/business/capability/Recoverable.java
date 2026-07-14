package com.flechazo.hkt.business.capability;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a computation with an explicit recoverable error channel.
 *
 * @param <E> the error type
 * @param <A> the result type
 */
public interface Recoverable<E, A> extends Chainable<A> {
    /**
     * Recovers from an error by producing a replacement result.
     *
     * @param recovery the function producing a result from an error
     * @return a computation with the recovery operation attached
     */
    Recoverable<E, A> recover(Function<? super E, ? extends A> recovery);

    /**
     * Recovers from an error by selecting another computation.
     *
     * @param recovery the function selecting a replacement computation
     * @return a computation with the recovery operation attached
     */
    Recoverable<E, A> recoverWith(Function<? super E, ? extends Recoverable<E, A>> recovery);

    /**
     * Selects an alternative computation when this computation cannot produce a result.
     *
     * @param alternative the deferred alternative computation
     * @return a computation with the alternative attached
     */
    Recoverable<E, A> orElse(Supplier<? extends Recoverable<E, A>> alternative);

    /**
     * Transforms the explicit error value.
     *
     * @param <E2> the transformed error type
     * @param mapper the error transformation
     * @return a computation with the transformed error channel
     */
    <E2> Recoverable<E2, A> mapError(Function<? super E, ? extends E2> mapper);
}

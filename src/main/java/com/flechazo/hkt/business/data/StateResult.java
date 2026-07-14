package com.flechazo.hkt.business.data;

/**
 * Contains the final state and result of a stateful computation.
 *
 * @param <S> the state type
 * @param <A> the result type
 * @param state the final state
 * @param value the computed result
 */
public record StateResult<S, A>(S state, A value) {
}

package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * Represents a serializable function that rebuilds a lens source with a replacement focus.
 *
 * @param <S> the source type
 * @param <A> the replacement focus type
 * @param <T> the rebuilt source type
 */
@FunctionalInterface
public interface LensRebuilder<S, A, T> extends BiFunction<S, A, T>, Serializable {
}

package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a serializable setter operation that applies a focus transformation to a source.
 *
 * @param <S> the input source type
 * @param <T> the rebuilt source type
 * @param <A> the input focus type
 * @param <B> the replacement focus type
 */
@FunctionalInterface
public interface SetterModifier<S, T, A, B>
        extends BiFunction<Function<? super A, ? extends B>, S, T>, Serializable {
}

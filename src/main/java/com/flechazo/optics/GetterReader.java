package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Represents a serializable function that observes a value from a source.
 *
 * @param <S> the source type
 * @param <A> the observed value type
 */
@FunctionalInterface
public interface GetterReader<S, A> extends Function<S, A>, Serializable {
}

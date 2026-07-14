package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Represents a serializable function that builds a prism source from a focus.
 *
 * @param <A> the focus type
 * @param <S> the source type
 */
@FunctionalInterface
public interface PrismBuilder<A, S> extends Function<A, S>, Serializable {
}

package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Represents a serializable function that rebuilds an isomorphism source from a focus.
 *
 * @param <A> the focus type
 * @param <S> the rebuilt source type
 */
@FunctionalInterface
public interface IsoRebuilder<A, S> extends Function<A, S>, Serializable {
}

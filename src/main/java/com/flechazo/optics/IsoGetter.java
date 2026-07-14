package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Represents a serializable function that reads the focus of an isomorphism.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface IsoGetter<S, A> extends Function<S, A>, Serializable {
}

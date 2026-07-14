package com.flechazo.optics;

import com.flechazo.hkt.Either;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Represents a serializable prism match that returns either a nonmatching source or a focus.
 *
 * @param <S> the input source type
 * @param <T> the nonmatching rebuilt source type
 * @param <A> the matched focus type
 */
@FunctionalInterface
public interface PrismMatcher<S, T, A> extends Function<S, Either<T, A>>, Serializable {
}

package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Represents a serializable function that enumerates the focuses observed by a fold.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface FoldGetter<S, A> extends Function<S, Iterable<? extends A>>, Serializable {
}

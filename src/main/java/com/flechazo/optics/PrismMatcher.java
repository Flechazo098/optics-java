package com.flechazo.optics;

import com.flechazo.hkt.Either;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface PrismMatcher<S, T, A> extends Function<S, Either<T, A>>, Serializable {
}

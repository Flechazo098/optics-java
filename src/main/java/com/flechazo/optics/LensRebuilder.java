package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.BiFunction;

@FunctionalInterface
public interface LensRebuilder<S, A, T> extends BiFunction<S, A, T>, Serializable {
}

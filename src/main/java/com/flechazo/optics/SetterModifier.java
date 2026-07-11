package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface SetterModifier<S, T, A, B>
        extends BiFunction<Function<? super A, ? extends B>, S, T>, Serializable {
}

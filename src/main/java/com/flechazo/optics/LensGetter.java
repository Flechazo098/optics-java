package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface LensGetter<S, A> extends Function<S, A>, Serializable {
}

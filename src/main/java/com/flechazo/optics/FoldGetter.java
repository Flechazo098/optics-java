package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface FoldGetter<S, A> extends Function<S, Iterable<? extends A>>, Serializable {
}

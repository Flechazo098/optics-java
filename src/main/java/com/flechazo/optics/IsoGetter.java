package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface IsoGetter<S, A> extends Function<S, A>, Serializable {
}

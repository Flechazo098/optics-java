package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface GetterReader<S, A> extends Function<S, A>, Serializable {
}

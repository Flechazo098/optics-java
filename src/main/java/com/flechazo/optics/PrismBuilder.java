package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface PrismBuilder<A, S> extends Function<A, S>, Serializable {
}

package com.flechazo.optics;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface IsoRebuilder<A, S> extends Function<A, S>, Serializable {
}

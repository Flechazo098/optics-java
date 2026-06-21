package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.BinaryOperator;

@FunctionalInterface
public interface Semigroup<A> {
    A combine(A left, A right);

    static <A> Semigroup<A> of(BinaryOperator<A> combine) {
        Objects.requireNonNull(combine, "combine");
        return (left, right) ->
                Objects.requireNonNull(
                        combine.apply(
                                Objects.requireNonNull(left, "left"),
                                Objects.requireNonNull(right, "right")),
                        "combine result");
    }
}

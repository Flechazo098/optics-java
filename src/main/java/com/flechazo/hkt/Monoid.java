package com.flechazo.hkt;

import com.flechazo.hkt.tuple.Tuple2;

import java.util.Objects;
import java.util.function.BinaryOperator;

public interface Monoid<A> extends Semigroup<A> {
    A empty();

    default A combineAll(Iterable<? extends A> values) {
        A result = empty();
        for (A value : values) {
            result = combine(result, value);
        }
        return result;
    }

    static <A> Monoid<A> of(A empty, BinaryOperator<A> combine) {
        Objects.requireNonNull(empty, "empty");
        Objects.requireNonNull(combine, "combine");
        return new Monoid<>() {
            @Override
            public A empty() {
                return empty;
            }

            @Override
            public A combine(A left, A right) {
                return Objects.requireNonNull(
                        combine.apply(
                                Objects.requireNonNull(left, "left"),
                                Objects.requireNonNull(right, "right")),
                        "combine result");
            }
        };
    }

    static <A, B> Monoid<Tuple2<A, B>> product(Monoid<A> first, Monoid<B> second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return Monoid.of(
                Tuple2.of(first.empty(), second.empty()),
                (left, right) ->
                        Tuple2.of(
                                first.combine(left.first(), right.first()),
                                second.combine(left.second(), right.second())));
    }
}

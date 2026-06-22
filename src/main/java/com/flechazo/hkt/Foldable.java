package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Foldable<T extends K1> {
    <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<T, A> value);

    default <A, B> B foldLeft(B initial, BiFunction<? super B, ? super A, ? extends B> f, App<T, A> value) {
        Objects.requireNonNull(f, "f");
        Endo<B> endo = foldMap(
                Endo.monoid(),
                a -> current -> f.apply(current, a),
                value);
        return endo.apply(initial);
    }

    @FunctionalInterface
    interface Endo<A> extends Function<A, A> {
        static <A> Monoid<Endo<A>> monoid() {
            return new Monoid<>() {
                @Override
                public Endo<A> empty() {
                    return value -> value;
                }

                @Override
                public Endo<A> combine(Endo<A> left, Endo<A> right) {
                    return left.andThen(right)::apply;
                }
            };
        }
    }
}

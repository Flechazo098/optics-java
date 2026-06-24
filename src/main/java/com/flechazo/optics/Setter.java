package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface Setter<S, T, A, B> extends Optic<S, T, A, B> {
    T modify(Function<? super A, ? extends B> f, S source);

    default T set(B value, S source) {
        return modify(ignored -> value, source);
    }

    @Override
    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
        throw new UnsupportedOperationException(
                "modifyF is not supported by a pure Setter. Use Setter.fromGetSet(), Lens.asSetter(), "
                        + "or a Setter implementation that explicitly supports effectful modification.");
    }

    default <C, D> Setter<S, T, C, D> andThen(Setter<A, B, C, D> other) {
        Setter<S, T, A, B> self = this;
        return new Setter<>() {
            @Override
            public T modify(Function<? super C, ? extends D> f, S source) {
                return self.modify(value -> other.modify(f, value), source);
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }
        };
    }

    default <C, D> Setter<S, T, C, D> andThen(Lens<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    default <C, D> Setter<S, T, C, D> andThen(Affine<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    default <C, D> Setter<S, T, C, D> andThen(Traversal<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    default <C, D> Setter<S, T, C, D> andThen(Prism<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    static <S, T, A, B> Setter<S, T, A, B> of(
            BiFunction<Function<? super A, ? extends B>, S, T> modify) {
        return modify::apply;
    }

    static <S, T, A, B> Setter<S, T, A, B> fromGetSet(
            Function<? super S, ? extends A> getter,
            BiFunction<S, B, T> setter) {
        return new Setter<>() {
            @Override
            public T modify(Function<? super A, ? extends B> f, S source) {
                return setter.apply(source, f.apply(getter.apply(source)));
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return applicative.map(value -> setter.apply(source, value), f.apply(getter.apply(source)));
            }
        };
    }

    static <S> Setter<S, S, S, S> identity() {
        return new Setter<>() {
            @Override
            public S modify(Function<? super S, ? extends S> f, S source) {
                return f.apply(source);
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<S, App<F, S>> f, S source, Applicative<F, ?> applicative) {
                return f.apply(source);
            }
        };
    }
}

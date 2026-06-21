package com.flechazo.optics;

import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.App;
import com.flechazo.hkt.K1;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface Setter<S, A> extends Optic<S, S, A, A> {
    S modify(Function<? super A, ? extends A> f, S source);

    default S set(A value, S source) {
        return modify(ignored -> value, source);
    }

    @Override
    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
        throw new UnsupportedOperationException(
                "modifyF is not supported by a pure Setter. Use Setter.fromGetSet(), Lens.asSetter(), "
                        + "or a Setter implementation that explicitly supports effectful modification.");
    }

    default <B> Setter<S, B> andThen(Setter<A, B> other) {
        Setter<S, A> self = this;
        return new Setter<>() {
            @Override
            public S modify(Function<? super B, ? extends B> f, S source) {
                return self.modify(value -> other.modify(f, value), source);
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }
        };
    }

    default <B> Setter<S, B> andThen(Lens<A, B> other) {
        return andThen(other.asSetter());
    }

    default <B> Setter<S, B> andThen(Affine<A, B> other) {
        return andThen(other.asSetter());
    }

    default <B> Setter<S, B> andThen(Traversal<A, B> other) {
        return andThen(other.asSetter());
    }

    default <B> Setter<S, B> andThen(Prism<A, B> other) {
        return andThen(other.asSetter());
    }

    default <B> Setter<S, B> andThen(Iso<A, B> other) {
        return andThen(other.asSetter());
    }

    static <S, A> Setter<S, A> of(BiFunction<Function<? super A, ? extends A>, S, S> modify) {
        return modify::apply;
    }

    static <S, A> Setter<S, A> fromGetSet(Function<? super S, ? extends A> getter, BiFunction<S, A, S> setter) {
        return new Setter<>() {
            @Override
            public S modify(Function<? super A, ? extends A> f, S source) {
                return setter.apply(source, f.apply(getter.apply(source)));
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
                return applicative.map(value -> setter.apply(source, value), f.apply(getter.apply(source)));
            }
        };
    }

    static <S> Setter<S, S> identity() {
        return new Setter<>() {
            @Override
            public S modify(Function<? super S, ? extends S> f, S source) {
                return f.apply(source);
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<S, App<F, S>> f, S source, Applicative<F> applicative) {
                return f.apply(source);
            }
        };
    }
}

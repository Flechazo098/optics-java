package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;

import java.util.function.Function;

public interface Optic<S, T, A, B> {
    <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative);

    default <C, D> Optic<S, T, C, D> andThen(Optic<A, B, C, D> other) {
        Optic<S, T, A, B> self = this;
        return new Optic<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(a -> other.modifyF(f, a, applicative), source, applicative);
            }
        };
    }

    default <U> Optic<S, U, A, B> map(Function<? super T, ? extends U> f) {
        Optic<S, T, A, B> self = this;
        return new Optic<>() {
            @Override
            public <F extends K1> App<F, U> modifyF(
                    Function<A, App<F, B>> g, S source, Applicative<F, ?> applicative) {
                return applicative.map(f, self.modifyF(g, source, applicative));
            }
        };
    }

    default <R> Optic<R, T, A, B> contramap(Function<? super R, ? extends S> f) {
        Optic<S, T, A, B> self = this;
        return new Optic<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> g, R source, Applicative<F, ?> applicative) {
                return self.modifyF(g, f.apply(source), applicative);
            }
        };
    }

    default <R, U> Optic<R, U, A, B> dimap(
            Function<? super R, ? extends S> before, Function<? super T, ? extends U> after) {
        Optic<S, T, A, B> self = this;
        return new Optic<>() {
            @Override
            public <F extends K1> App<F, U> modifyF(
                    Function<A, App<F, B>> f, R source, Applicative<F, ?> applicative) {
                return applicative.map(after, self.modifyF(f, before.apply(source), applicative));
            }
        };
    }
}

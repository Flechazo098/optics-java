package com.flechazo.optics;

import com.flechazo.hkt.Monoid;

import java.util.function.Function;

@FunctionalInterface
public interface Getter<S, A> extends Fold<S, A> {
    A get(S source);

    @Override
    default <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
        return f.apply(get(source));
    }

    default <B> Getter<S, B> andThen(Getter<A, B> other) {
        return source -> other.get(get(source));
    }

    default <B> Getter<S, B> andThen(Lens<A, B> other) {
        return source -> other.get(get(source));
    }

    default <B> Getter<S, B> andThen(Iso<A, B> other) {
        return source -> other.get(get(source));
    }

    default <B> Fold<S, B> andThen(Prism<A, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(Affine<A, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(Traversal<A, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        Getter<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super B, ? extends M> f, S source) {
                return other.foldMap(monoid, f, self.get(source));
            }
        };
    }

    static <S, A> Getter<S, A> of(Function<? super S, ? extends A> getter) {
        return getter::apply;
    }
}

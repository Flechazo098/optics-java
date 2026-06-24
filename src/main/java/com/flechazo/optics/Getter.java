package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.functions.PointFreeFold;

import java.util.function.Function;

@FunctionalInterface
public interface Getter<S, A> extends Fold<S, A> {
    A get(S source);

    @Override
    default <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
        return f.apply(get(source));
    }

    default <B> Getter<S, B> andThen(Getter<A, B> other) {
        Getter<S, A> self = this;
        return new Getter<>() {
            @Override
            public B get(S source) {
                return other.get(self.get(source));
            }

            @Override
            public Maybe<PointFreeFold<S, B>> typedFold() {
                return self.typedFold()
                        .flatMap(left -> other.typedFold().map(right -> left.andThen(right)));
            }
        };
    }

    default <B> Getter<S, B> andThen(Lens<A, A, B, B> other) {
        return andThen(other.asGetter());
    }

    default <B> Getter<S, B> andThen(Iso<A, B> other) {
        return source -> other.get(get(source));
    }

    default <B> Fold<S, B> andThen(Prism<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(Affine<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(Traversal<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        Getter<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super B, ? extends M> f, S source) {
                return other.foldMap(monoid, f, self.get(source));
            }

            @Override
            public Maybe<PointFreeFold<S, B>> typedFold() {
                return self.typedFold()
                        .flatMap(left -> other.typedFold().map(right -> left.andThen(right)));
            }
        };
    }

    static <S, A> Getter<S, A> of(Function<? super S, ? extends A> getter) {
        return getter::apply;
    }
}

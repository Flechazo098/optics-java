package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface ReForgetP<R, A, B> extends App2<ReForgetP.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> ReForgetP<R, A, B> of(BiFunction<? super A, ? super R, ? extends B> function) {
        Objects.requireNonNull(function, "function");
        return function::apply;
    }

    static <R, A, B> ReForgetP<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (ReForgetP<R, A, B>) Objects.requireNonNull(value, "value");
    }

    B run(A value, R context);

    final class Instance<R> implements AffineP<ReForgetP.Mu<R>, Instance.Mu> {
        public static final class Mu implements AffineP.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<ReForgetP.Mu<R>, A, B>, App2<ReForgetP.Mu<R>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            return FunctionArrow.of(value -> {
                ReForgetP<R, A, B> reForget = ReForgetP.unbox(value);
                return ReForgetP.of((input, context) -> right.apply(reForget.run(left.apply(input), context)));
            });
        }

        @Override
        public <A, B, C> App2<ReForgetP.Mu<R>, Pair<A, C>, Pair<B, C>> first(
                App2<ReForgetP.Mu<R>, A, B> value) {
            ReForgetP<R, A, B> reForget = ReForgetP.unbox(value);
            return ReForgetP.of((pair, context) -> Pair.of(reForget.run(pair.first(), context), pair.second()));
        }

        @Override
        public <A, B, C> App2<ReForgetP.Mu<R>, Either<A, C>, Either<B, C>> left(
                App2<ReForgetP.Mu<R>, A, B> value) {
            ReForgetP<R, A, B> reForget = ReForgetP.unbox(value);
            return ReForgetP.of((either, context) -> either.mapLeft(left -> reForget.run(left, context)));
        }
    }
}

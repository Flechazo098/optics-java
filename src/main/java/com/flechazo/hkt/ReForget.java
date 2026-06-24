package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ReForget<R, A, B> extends App2<ReForget.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> ReForget<R, A, B> of(Function<? super R, ? extends B> function) {
        Objects.requireNonNull(function, "function");
        return function::apply;
    }

    static <R, A, B> ReForget<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (ReForget<R, A, B>) Objects.requireNonNull(value, "value");
    }

    B run(R value);

    final class Instance<R> implements ReCartesian<ReForget.Mu<R>, Instance.Mu>, Cocartesian<ReForget.Mu<R>, Instance.Mu> {
        public static final class Mu implements ReCartesian.Mu, Cocartesian.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<ReForget.Mu<R>, A, B>, App2<ReForget.Mu<R>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(right, "right");
            return FunctionArrow.of(value -> {
                ReForget<R, A, B> reForget = ReForget.unbox(value);
                return ReForget.of(input -> right.apply(reForget.run(input)));
            });
        }

        @Override
        public <A, B, C> App2<ReForget.Mu<R>, A, B> unfirst(
                App2<ReForget.Mu<R>, Pair<A, C>, Pair<B, C>> input) {
            ReForget<R, Pair<A, C>, Pair<B, C>> reForget = ReForget.unbox(input);
            return ReForget.of(value -> reForget.run(value).first());
        }

        @Override
        public <A, B, C> App2<ReForget.Mu<R>, A, B> unsecond(
                App2<ReForget.Mu<R>, Pair<C, A>, Pair<C, B>> input) {
            ReForget<R, Pair<C, A>, Pair<C, B>> reForget = ReForget.unbox(input);
            return ReForget.of(value -> reForget.run(value).second());
        }

        @Override
        public <A, B, C> App2<ReForget.Mu<R>, Either<A, C>, Either<B, C>> left(
                App2<ReForget.Mu<R>, A, B> value) {
            ReForget<R, A, B> reForget = ReForget.unbox(value);
            return ReForget.of(input -> Either.left(reForget.run(input)));
        }
    }
}

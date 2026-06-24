package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ReForgetEP<R, A, B> extends App2<ReForgetEP.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> ReForgetEP<R, A, B> of(Function<? super Either<A, Pair<A, R>>, ? extends B> function) {
        Objects.requireNonNull(function, "function");
        return function::apply;
    }

    static <R, A, B> ReForgetEP<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (ReForgetEP<R, A, B>) Objects.requireNonNull(value, "value");
    }

    B run(Either<A, Pair<A, R>> value);

    final class Instance<R> implements AffineP<ReForgetEP.Mu<R>, Instance.Mu> {
        public static final class Mu implements AffineP.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<ReForgetEP.Mu<R>, A, B>, App2<ReForgetEP.Mu<R>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            return FunctionArrow.of(value -> {
                ReForgetEP<R, A, B> reForget = ReForgetEP.unbox(value);
                return ReForgetEP.of(either -> right.apply(reForget.run(either.mapBoth(
                        left,
                        pair -> Pair.of(left.apply(pair.first()), pair.second())))));
            });
        }

        @Override
        public <A, B, C> App2<ReForgetEP.Mu<R>, Pair<A, C>, Pair<B, C>> first(
                App2<ReForgetEP.Mu<R>, A, B> value) {
            ReForgetEP<R, A, B> reForget = ReForgetEP.unbox(value);
            return ReForgetEP.of(either -> either.fold(
                    pair -> Pair.of(reForget.run(Either.left(pair.first())), pair.second()),
                    pair -> Pair.of(
                            reForget.run(Either.right(Pair.of(pair.first().first(), pair.second()))),
                            pair.first().second())));
        }

        @Override
        public <A, B, C> App2<ReForgetEP.Mu<R>, Either<A, C>, Either<B, C>> left(
                App2<ReForgetEP.Mu<R>, A, B> value) {
            ReForgetEP<R, A, B> reForget = ReForgetEP.unbox(value);
            return ReForgetEP.of(either -> either.fold(
                    nested -> nested.mapLeft(left -> reForget.run(Either.left(left))),
                    pair -> pair.first().mapLeft(left -> reForget.run(Either.right(Pair.of(left, pair.second()))))));
        }
    }
}

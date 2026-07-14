package com.flechazo.hkt;

import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ReForgetEP<R, A, B> extends App2<ReForgetEP.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> ReForgetEP<R, A, B> of(Function<? super Either<A, Tuple2<A, R>>, ? extends B> function) {
        Objects.requireNonNull(function, "function");
        return function::apply;
    }

    static <R, A, B> ReForgetEP<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (ReForgetEP<R, A, B>) Validation.kind().narrowWithTypeCheck2(value, ReForgetEP.class);
    }

    B run(Either<A, Tuple2<A, R>> value);

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
                        tuple -> Tuple2.of(left.apply(tuple.first()), tuple.second())))));
            });
        }

        @Override
        public <A, B, C> App2<ReForgetEP.Mu<R>, Tuple2<A, C>, Tuple2<B, C>> first(
                App2<ReForgetEP.Mu<R>, A, B> value) {
            ReForgetEP<R, A, B> reForget = ReForgetEP.unbox(value);
            return ReForgetEP.of(either -> either.fold(
                    tuple -> Tuple2.of(reForget.run(Either.left(tuple.first())), tuple.second()),
                    tuple -> Tuple2.of(
                            reForget.run(Either.right(Tuple2.of(tuple.first().first(), tuple.second()))),
                            tuple.first().second())));
        }

        @Override
        public <A, B, C> App2<ReForgetEP.Mu<R>, Either<A, C>, Either<B, C>> left(
                App2<ReForgetEP.Mu<R>, A, B> value) {
            ReForgetEP<R, A, B> reForget = ReForgetEP.unbox(value);
            return ReForgetEP.of(either -> either.fold(
                    nested -> nested.mapLeft(left -> reForget.run(Either.left(left))),
                    tuple -> tuple.first().mapLeft(left -> reForget.run(Either.right(Tuple2.of(left, tuple.second()))))));
        }
    }
}

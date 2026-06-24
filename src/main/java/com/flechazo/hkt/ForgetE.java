package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ForgetE<R, A, B> extends App2<ForgetE.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> ForgetE<R, A, B> of(Function<? super A, Either<B, R>> function) {
        Objects.requireNonNull(function, "function");
        return function::apply;
    }

    static <R, A, B> ForgetE<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (ForgetE<R, A, B>) Objects.requireNonNull(value, "value");
    }

    Either<B, R> run(A value);

    final class Instance<R> implements AffineP<ForgetE.Mu<R>, Instance.Mu> {
        public static final class Mu implements AffineP.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<ForgetE.Mu<R>, A, B>, App2<ForgetE.Mu<R>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            return FunctionArrow.of(value -> {
                ForgetE<R, A, B> forget = ForgetE.unbox(value);
                return ForgetE.of(input -> forget.run(left.apply(input)).mapLeft(right));
            });
        }

        @Override
        public <A, B, C> App2<ForgetE.Mu<R>, Pair<A, C>, Pair<B, C>> first(
                App2<ForgetE.Mu<R>, A, B> value) {
            ForgetE<R, A, B> forget = ForgetE.unbox(value);
            return ForgetE.of(pair -> forget.run(pair.first()).mapLeft(result -> Pair.of(result, pair.second())));
        }

        @Override
        public <A, B, C> App2<ForgetE.Mu<R>, Either<A, C>, Either<B, C>> left(
                App2<ForgetE.Mu<R>, A, B> value) {
            ForgetE<R, A, B> forget = ForgetE.unbox(value);
            return ForgetE.of(either -> either.fold(
                    left -> forget.run(left).mapLeft(Either::left),
                    right -> Either.left(Either.right(right))));
        }
    }
}

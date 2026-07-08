package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ForgetOpt<R, A, B> extends App2<ForgetOpt.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> ForgetOpt<R, A, B> of(Function<? super A, Maybe<R>> function) {
        Objects.requireNonNull(function, "function");
        return function::apply;
    }

    static <R, A, B> ForgetOpt<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (ForgetOpt<R, A, B>) Validation.kind().narrowWithTypeCheck2(value, ForgetOpt.class);
    }

    Maybe<R> run(A value);

    final class Instance<R> implements AffineP<ForgetOpt.Mu<R>, Instance.Mu> {
        public static final class Mu implements AffineP.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<ForgetOpt.Mu<R>, A, B>, App2<ForgetOpt.Mu<R>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            return FunctionArrow.of(value -> {
                ForgetOpt<R, A, B> forget = ForgetOpt.unbox(value);
                return ForgetOpt.of(input -> forget.run(left.apply(input)));
            });
        }

        @Override
        public <A, B, C> App2<ForgetOpt.Mu<R>, Tuple2<A, C>, Tuple2<B, C>> first(
                App2<ForgetOpt.Mu<R>, A, B> value) {
            ForgetOpt<R, A, B> forget = ForgetOpt.unbox(value);
            return ForgetOpt.of(tuple -> forget.run(tuple.first()));
        }

        @Override
        public <A, B, C> App2<ForgetOpt.Mu<R>, Either<A, C>, Either<B, C>> left(
                App2<ForgetOpt.Mu<R>, A, B> value) {
            ForgetOpt<R, A, B> forget = ForgetOpt.unbox(value);
            return ForgetOpt.of(either -> either.isLeft() ? forget.run(either.left()) : Maybe.none());
        }
    }
}

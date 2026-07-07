package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface Forget<R, A, B> extends App2<Forget.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> Forget<R, A, B> of(Function<? super A, ? extends R> function) {
        Objects.requireNonNull(function, "function");
        return function::apply;
    }

    static <R, A, B> Forget<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (Forget<R, A, B>) Objects.requireNonNull(value, "value");
    }

    R run(A value);

    final class Instance<R> implements Cartesian<Forget.Mu<R>, Instance.Mu>, ReCocartesian<Forget.Mu<R>, Instance.Mu> {
        public static final class Mu implements Cartesian.Mu, ReCocartesian.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<Forget.Mu<R>, A, B>, App2<Forget.Mu<R>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            return FunctionArrow.of(value -> {
                Forget<R, A, B> forget = Forget.unbox(value);
                return Forget.of(input -> forget.run(left.apply(input)));
            });
        }

        @Override
        public <A, B, C> App2<Forget.Mu<R>, Tuple2<A, C>, Tuple2<B, C>> first(
                App2<Forget.Mu<R>, A, B> value) {
            Forget<R, A, B> forget = Forget.unbox(value);
            return Forget.of(tuple -> forget.run(tuple.first()));
        }

        @Override
        public <A, B, C> App2<Forget.Mu<R>, A, B> unleft(
                App2<Forget.Mu<R>, Either<A, C>, Either<B, C>> input) {
            Forget<R, Either<A, C>, Either<B, C>> forget = Forget.unbox(input);
            return Forget.of(value -> forget.run(Either.left(value)));
        }

        @Override
        public <A, B, C> App2<Forget.Mu<R>, A, B> unright(
                App2<Forget.Mu<R>, Either<C, A>, Either<C, B>> input) {
            Forget<R, Either<C, A>, Either<C, B>> forget = Forget.unbox(input);
            return Forget.of(value -> forget.run(Either.right(value)));
        }
    }
}

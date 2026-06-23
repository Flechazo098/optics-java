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

    final class Instance<R> implements Cartesian<Forget.Mu<R>, Instance.Mu> {
        public static final class Mu implements Cartesian.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> App2<Forget.Mu<R>, C, D> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right,
                App2<Forget.Mu<R>, A, B> value) {
            Objects.requireNonNull(left, "left");
            Forget<R, A, B> forget = Forget.unbox(value);
            return Forget.of(input -> forget.run(left.apply(input)));
        }

        @Override
        public <A, B, C> App2<Forget.Mu<R>, Pair<A, C>, Pair<B, C>> first(
                App2<Forget.Mu<R>, A, B> value) {
            Forget<R, A, B> forget = Forget.unbox(value);
            return Forget.of(pair -> forget.run(pair.first()));
        }

        @Override
        public <A, B, C> App2<Forget.Mu<R>, Pair<C, A>, Pair<C, B>> second(
                App2<Forget.Mu<R>, A, B> value) {
            Forget<R, A, B> forget = Forget.unbox(value);
            return Forget.of(pair -> forget.run(pair.second()));
        }
    }
}

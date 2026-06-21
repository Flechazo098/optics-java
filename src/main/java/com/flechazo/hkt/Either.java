package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public sealed interface Either<L, R> extends App2<Either.Mu, L, R> permits Either.Left, Either.Right {
    final class Mu implements K2 {
        private Mu() {
        }
    }

    boolean isLeft();

    default boolean isRight() {
        return !isLeft();
    }

    @Nullable L left();

    @Nullable R right();

    default <B> Either<L, B> map(Function<? super R, ? extends B> f) {
        return isRight() ? right(f.apply(right())) : left(left());
    }

    default <B> Either<L, B> flatMap(Function<? super R, Either<L, B>> f) {
        return isRight() ? Objects.requireNonNull(f.apply(right()), "flatMap result") : left(left());
    }

    static <L, R> Either<L, R> left(@Nullable L value) {
        return new Left<>(value);
    }

    static <L, R> Either<L, R> right(@Nullable R value) {
        return new Right<>(value);
    }

    static <L, R> Either<L, R> narrow(App<App2.Mu<Mu, L>, R> value) {
        return (Either<L, R>) Objects.requireNonNull(value, "value");
    }

    static <L, R> Either<L, R> narrow2(App2<Mu, L, R> value) {
        return (Either<L, R>) Objects.requireNonNull(value, "value");
    }

    @SuppressWarnings("unchecked")
    static <L> Applicative<App2.Mu<Mu, L>> applicative() {
        return (Applicative<App2.Mu<Mu, L>>) (Applicative<?>) EitherMonad.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <L> Monad<App2.Mu<Mu, L>> monad() {
        return (Monad<App2.Mu<Mu, L>>) (Monad<?>) EitherMonad.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <L> Selective<App2.Mu<Mu, L>> selective() {
        return (Selective<App2.Mu<Mu, L>>) (Selective<?>) EitherMonad.INSTANCE;
    }

    record Left<L, R>(@Nullable L value) implements Either<L, R> {
        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public @Nullable L left() {
            return value;
        }

        @Override
        public @Nullable R right() {
            throw new IllegalStateException("Either.left has no right value");
        }
    }

    record Right<L, R>(@Nullable R value) implements Either<L, R> {
        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public @Nullable L left() {
            throw new IllegalStateException("Either.right has no left value");
        }

        @Override
        public @Nullable R right() {
            return value;
        }
    }

    enum EitherMonad implements Monad<App2.Mu<Mu, Object>>, Selective<App2.Mu<Mu, Object>> {
        INSTANCE;

        @Override
        public <A> App<App2.Mu<Mu, Object>, A> of(@Nullable A value) {
            return Either.right(value);
        }

        @Override
        public <A, B> App<App2.Mu<Mu, Object>, B> flatMap(
                Function<? super A, ? extends App<App2.Mu<Mu, Object>, B>> f,
                App<App2.Mu<Mu, Object>, A> fa) {
            Either<Object, A> either = Either.narrow(fa);
            return either.isRight()
                    ? Objects.requireNonNull(f.apply(either.right()), "flatMap result")
                    : Either.left(either.left());
        }

        @Override
        public <A, B> App<App2.Mu<Mu, Object>, B> select(
                App<App2.Mu<Mu, Object>, Either<A, B>> value,
                App<App2.Mu<Mu, Object>, ? extends Function<A, B>> function) {
            Either<Object, Either<A, B>> either = Either.narrow(value);
            if (either.isLeft()) {
                return Either.left(either.left());
            }
            Either<A, B> inner = Objects.requireNonNull(either.right(), "select value");
            if (inner.isRight()) {
                return Either.right(inner.right());
            }
            Either<Object, ? extends Function<A, B>> fn = Either.narrow(function);
            if (fn.isLeft()) {
                return Either.left(fn.left());
            }
            return Either.right(Objects.requireNonNull(fn.right(), "select function").apply(inner.left()));
        }

        @Override
        public <A> App<App2.Mu<Mu, Object>, A> ifS(
                App<App2.Mu<Mu, Object>, Boolean> condition,
                Supplier<? extends App<App2.Mu<Mu, Object>, A>> thenValue,
                Supplier<? extends App<App2.Mu<Mu, Object>, A>> elseValue) {
            Either<Object, Boolean> test = Either.narrow(condition);
            if (test.isLeft()) {
                return Either.left(test.left());
            }
            Supplier<? extends App<App2.Mu<Mu, Object>, A>> branch =
                    Boolean.TRUE.equals(test.right()) ? thenValue : elseValue;
            return Objects.requireNonNull(branch.get(), "ifS branch result");
        }
    }
}

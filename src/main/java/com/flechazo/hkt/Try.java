package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Try<A> extends App<Try.Mu, A> permits Try.Success, Try.Failure {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    A get();

    Throwable cause();

    default <B> Try<B> map(Function<? super A, ? extends B> f) {
        if (isFailure()) {
            return failure(cause());
        }
        return of(() -> f.apply(get()));
    }

    default <B> Try<B> flatMap(Function<? super A, Try<B>> f) {
        if (isFailure()) {
            return failure(cause());
        }
        try {
            return Objects.requireNonNull(f.apply(get()), "flatMap result");
        } catch (Exception exception) {
            return failure(exception);
        }
    }

    static <A> Try<A> of(ThrowingSupplier<? extends A> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return success(Objects.requireNonNull(supplier.get(), "supplier result"));
        } catch (Exception exception) {
            return failure(exception);
        }
    }

    static <A> Try<A> success(A value) {
        return new Success<>(Objects.requireNonNull(value, "value"));
    }

    static <A> Try<A> failure(Throwable cause) {
        return new Failure<>(cause);
    }

    static <A> Try<A> unbox(App<Mu, A> value) {
        return (Try<A>) Objects.requireNonNull(value, "value");
    }

    static Applicative<Try.Mu, TryMonad.MuProof> applicative() {
        return TryMonad.INSTANCE;
    }

    static Monad<Try.Mu, TryMonad.MuProof> monad() {
        return TryMonad.INSTANCE;
    }

    static Selective<Try.Mu, TryMonad.MuProof> selective() {
        return TryMonad.INSTANCE;
    }

    record Success<A>(A value) implements Try<A> {
        public Success {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public A get() {
            return value;
        }

        @Override
        public Throwable cause() {
            throw new IllegalStateException("Try.success has no failure cause");
        }
    }

    record Failure<A>(Throwable cause) implements Try<A> {
        public Failure {
            Objects.requireNonNull(cause, "cause");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public A get() {
            throw new IllegalStateException("Try.failure has no success value", cause);
        }
    }

    enum TryMonad implements Monad<Try.Mu, TryMonad.MuProof>, Selective<Try.Mu, TryMonad.MuProof> {
        INSTANCE;

        static final class MuProof implements Applicative.Mu {
            private MuProof() {
            }
        }

        @Override
        public <A> App<Try.Mu, A> of(A value) {
            return Try.success(value);
        }

        @Override
        public <A, B> App<Try.Mu, B> flatMap(
                Function<? super A, ? extends App<Try.Mu, B>> f, App<Try.Mu, A> fa) {
            return Try.unbox(fa).flatMap(value -> Try.unbox(Objects.requireNonNull(f.apply(value), "flatMap result")));
        }

        @Override
        public <A, B> App<Try.Mu, B> select(
                App<Try.Mu, Either<A, B>> value, App<Try.Mu, ? extends Function<A, B>> function) {
            Try<Either<A, B>> either = Try.unbox(value);
            if (either.isFailure()) {
                return Try.failure(either.cause());
            }
            Either<A, B> inner = Objects.requireNonNull(either.get(), "select value");
            if (inner.isRight()) {
                return Try.success(inner.right());
            }
            Try<? extends Function<A, B>> fn = Try.unbox(function);
            if (fn.isFailure()) {
                return Try.failure(fn.cause());
            }
            return Try.of(() -> Objects.requireNonNull(fn.get(), "select function").apply(inner.left()));
        }

        @Override
        public <A> App<Try.Mu, A> ifS(
                App<Try.Mu, Boolean> condition,
                Supplier<? extends App<Try.Mu, A>> thenValue,
                Supplier<? extends App<Try.Mu, A>> elseValue) {
            Try<Boolean> test = Try.unbox(condition);
            if (test.isFailure()) {
                return Try.failure(test.cause());
            }
            Supplier<? extends App<Try.Mu, A>> branch = Boolean.TRUE.equals(test.get()) ? thenValue : elseValue;
            try {
                return Objects.requireNonNull(branch.get(), "ifS branch result");
            } catch (Exception exception) {
                return Try.failure(exception);
            }
        }
    }
}

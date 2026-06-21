package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public sealed interface Try<A> extends App<Try.Mu, A> permits Try.Success, Try.Failure {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    @Nullable A get();

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
            return success(supplier.get());
        } catch (Exception exception) {
            return failure(exception);
        }
    }

    static <A> Try<A> success(@Nullable A value) {
        return new Success<>(value);
    }

    static <A> Try<A> failure(Throwable cause) {
        return new Failure<>(cause);
    }

    static <A> Try<A> narrow(App<Mu, A> value) {
        return (Try<A>) Objects.requireNonNull(value, "value");
    }

    static Applicative<Mu> applicative() {
        return TryMonad.INSTANCE;
    }

    static Monad<Mu> monad() {
        return TryMonad.INSTANCE;
    }

    static Selective<Mu> selective() {
        return TryMonad.INSTANCE;
    }

    record Success<A>(@Nullable A value) implements Try<A> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public @Nullable A get() {
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
        public @Nullable A get() {
            throw new IllegalStateException("Try.failure has no success value", cause);
        }
    }

    enum TryMonad implements Monad<Mu>, Selective<Mu> {
        INSTANCE;

        @Override
        public <A> App<Mu, A> of(@Nullable A value) {
            return Try.success(value);
        }

        @Override
        public <A, B> App<Mu, B> flatMap(
                Function<? super A, ? extends App<Mu, B>> f, App<Mu, A> fa) {
            return Try.narrow(fa).flatMap(value -> Try.narrow(Objects.requireNonNull(f.apply(value), "flatMap result")));
        }

        @Override
        public <A, B> App<Mu, B> select(
                App<Mu, Either<A, B>> value, App<Mu, ? extends Function<A, B>> function) {
            Try<Either<A, B>> either = Try.narrow(value);
            if (either.isFailure()) {
                return Try.failure(either.cause());
            }
            Either<A, B> inner = Objects.requireNonNull(either.get(), "select value");
            if (inner.isRight()) {
                return Try.success(inner.right());
            }
            Try<? extends Function<A, B>> fn = Try.narrow(function);
            if (fn.isFailure()) {
                return Try.failure(fn.cause());
            }
            return Try.of(() -> Objects.requireNonNull(fn.get(), "select function").apply(inner.left()));
        }

        @Override
        public <A> App<Mu, A> ifS(
                App<Mu, Boolean> condition,
                Supplier<? extends App<Mu, A>> thenValue,
                Supplier<? extends App<Mu, A>> elseValue) {
            Try<Boolean> test = Try.narrow(condition);
            if (test.isFailure()) {
                return Try.failure(test.cause());
            }
            Supplier<? extends App<Mu, A>> branch = Boolean.TRUE.equals(test.get()) ? thenValue : elseValue;
            try {
                return Objects.requireNonNull(branch.get(), "ifS branch result");
            } catch (Exception exception) {
                return Try.failure(exception);
            }
        }
    }
}

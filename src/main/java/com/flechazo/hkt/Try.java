package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Consumer;
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

    default Try<A> peek(Consumer<? super A> action) {
        if (isSuccess()) action.accept(get());
        return this;
    }

    default Try<A> peekFailure(Consumer<? super Throwable> action) {
        if (isFailure()) action.accept(cause());
        return this;
    }

    default Try<A> recover(Function<? super Throwable, ? extends A> f) {
        if (isSuccess()) return this;
        return of(() -> f.apply(cause()));
    }

    default Try<A> recoverWith(Function<? super Throwable, Try<A>> f) {
        if (isSuccess()) return this;
        try { return Objects.requireNonNull(f.apply(cause()), "recoverWith"); }
        catch (Exception e) { return failure(e); }
    }

    default Try<A> mapError(Function<? super Throwable, ? extends Throwable> mapper) {
        return isSuccess() ? this : failure(mapper.apply(cause()));
    }

    default A orElse(A other) {
        return isSuccess() ? get() : other;
    }

    default A orElseGet(Supplier<? extends A> supplier) {
        return isSuccess() ? get() : supplier.get();
    }

    default <B> B fold(Function<? super Throwable, ? extends B> failure, Function<? super A, ? extends B> success) {
        return isSuccess() ? success.apply(get()) : failure.apply(cause());
    }

    default void match(Consumer<? super A> success, Consumer<? super Throwable> failure) {
        if (isSuccess()) {
            success.accept(get());
        } else {
            failure.accept(cause());
        }
    }

    default Maybe<A> toMaybe() {
        return isSuccess() ? Maybe.some(get()) : Maybe.none();
    }

    default Either<Throwable, A> toEither() {
        return isSuccess() ? Either.right(get()) : Either.left(cause());
    }

    default <E> Either<E, A> toEither(Function<? super Throwable, ? extends E> failureToLeft) {
        return isSuccess() ? Either.right(get()) : Either.left(failureToLeft.apply(cause()));
    }

    default <E> Validated<E, A> toValidated(Function<? super Throwable, ? extends E> failureToInvalid) {
        return isSuccess() ? Validated.valid(get()) : Validated.invalid(failureToInvalid.apply(cause()));
    }

    static <A> Try<A> of(CheckedSupplier<? extends A, ?> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return success(Objects.requireNonNull(supplier.get(), "supplier result"));
        } catch (Throwable error) {
            return failure(error);
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

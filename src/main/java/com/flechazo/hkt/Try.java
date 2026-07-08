package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.FLAT_MAP;
import static com.flechazo.hkt.util.validation.Operation.FOLD_MAP;
import static com.flechazo.hkt.util.validation.Operation.HANDLE_ERROR_WITH;
import static com.flechazo.hkt.util.validation.Operation.IF_S;
import static com.flechazo.hkt.util.validation.Operation.MAP;
import static com.flechazo.hkt.util.validation.Operation.RIGHT;
import static com.flechazo.hkt.util.validation.Operation.SELECT;
import static com.flechazo.hkt.util.validation.Operation.TRAVERSE;

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
        Validation.function().require(f, "f", FLAT_MAP);
        try {
            return Validation.function().requireNonNullResult(f.apply(get()), "f", FLAT_MAP);
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
        return new Success<>(Validation.coreType().requireValue(value, Try.class, RIGHT));
    }

    static <A> Try<A> failure(Throwable cause) {
        return new Failure<>(Validation.coreType().requireError(cause, Try.class, HANDLE_ERROR_WITH));
    }

    static <A> Try<A> unbox(App<Mu, A> value) {
        return (Try<A>) Validation.kind().narrowWithTypeCheck(value, Try.class);
    }

    static Applicative<Try.Mu, TryMonad.MuProof> applicative() {
        return TryMonad.INSTANCE;
    }

    static Monad<Try.Mu, TryMonad.MuProof> monad() {
        return TryMonad.INSTANCE;
    }

    static MonadError<Try.Mu, Throwable, TryMonad.MuProof> monadError() {
        return TryMonad.INSTANCE;
    }

    static Selective<Try.Mu, TryMonad.MuProof> selective() {
        return TryMonad.INSTANCE;
    }

    static Foldable<Try.Mu> foldable() {
        return TryMonad.INSTANCE;
    }

    static Traversable<Try.Mu, TryMonad.MuProof> traversable() {
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

    enum TryMonad implements MonadError<Try.Mu, Throwable, TryMonad.MuProof>,
            Selective<Try.Mu, TryMonad.MuProof>,
            Traversable<Try.Mu, TryMonad.MuProof> {
        INSTANCE;

        public static final class MuProof implements MonadError.Mu, Traversable.Mu {
            private MuProof() {
            }
        }

        @Override
        public <A> App<Try.Mu, A> of(A value) {
            return Try.success(value);
        }

        @Override
        public <A, B> App<Try.Mu, B> map(Function<? super A, ? extends B> f, App<Try.Mu, A> fa) {
            Validation.function().validateMap(f, fa);
            return Try.unbox(fa).map(f);
        }

        @Override
        public <A, B> App<Try.Mu, B> flatMap(
                Function<? super A, ? extends App<Try.Mu, B>> f, App<Try.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            return Try.unbox(fa).flatMap(value ->
                    Try.unbox(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP)));
        }

        @Override
        public <A> App<Try.Mu, A> raiseError(Throwable error) {
            return Try.failure(error);
        }

        @Override
        public <A> App<Try.Mu, A> handleErrorWith(
                App<Try.Mu, A> value,
                Function<? super Throwable, ? extends App<Try.Mu, A>> handler) {
            Validation.function().validateHandleErrorWith(value, handler);
            Try<A> attempt = Try.unbox(value);
            return attempt.isSuccess()
                    ? attempt
                    : Validation.function().requireNonNullResult(handler.apply(attempt.cause()), "handler", HANDLE_ERROR_WITH);
        }

        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<Try.Mu, A> value) {
            Validation.function().validateFoldMap(monoid, f, value);
            Try<A> attempt = Try.unbox(value);
            return attempt.isSuccess() ? f.apply(attempt.get()) : monoid.empty();
        }

        @Override
        public <F extends K1, A, B> App<F, App<Try.Mu, B>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, B>> f,
                App<Try.Mu, A> value) {
            Validation.function().validateTraverse(applicative, f, value);
            Try<A> attempt = Try.unbox(value);
            if (attempt.isFailure()) {
                return applicative.of(Try.failure(attempt.cause()));
            }
            return applicative.map(Try::success,
                    Validation.function().requireNonNullResult(f.apply(attempt.get()), "f", TRAVERSE));
        }

        @Override
        public <A, B> App<Try.Mu, B> select(
                App<Try.Mu, Either<A, B>> value, App<Try.Mu, ? extends Function<A, B>> function) {
            Try<Either<A, B>> either = Try.unbox(value);
            if (either.isFailure()) {
                return Try.failure(either.cause());
            }
            Either<A, B> inner = Validation.coreType().requireValue(either.get(), "select value", Try.class, SELECT);
            if (inner.isRight()) {
                return Try.success(inner.right());
            }
            Try<? extends Function<A, B>> fn = Try.unbox(function);
            if (fn.isFailure()) {
                return Try.failure(fn.cause());
            }
            return Try.of(() -> Validation.coreType().requireValue(
                    fn.get(),
                    "select function",
                    Try.class,
                    SELECT).apply(inner.left()));
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
                return Validation.function().requireNonNullResult(branch.get(), "branch", IF_S);
            } catch (Exception exception) {
                return Try.failure(exception);
            }
        }
    }
}

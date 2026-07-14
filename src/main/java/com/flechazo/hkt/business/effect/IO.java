package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.resilience.Bulkhead;
import com.flechazo.hkt.business.resilience.CircuitBreaker;
import com.flechazo.hkt.business.resilience.Retry;
import com.flechazo.hkt.business.resilience.RetryPolicy;
import com.flechazo.hkt.util.validation.Validation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.FLAT_MAP;
import static com.flechazo.hkt.util.validation.Operation.HANDLE_ERROR_WITH;
import static com.flechazo.hkt.util.validation.Operation.IF_S;
import static com.flechazo.hkt.util.validation.Operation.MAP;
import static com.flechazo.hkt.util.validation.Operation.MAP_ERROR;
import static com.flechazo.hkt.util.validation.Operation.RECOVER;
import static com.flechazo.hkt.util.validation.Operation.RECOVER_WITH;
import static com.flechazo.hkt.util.validation.Operation.SELECT;

@FunctionalInterface
public interface IO<A> extends App<IO.Mu, A> {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    A unsafeRun() throws Exception;

    default A unsafeRunSync() throws Exception {
        return unsafeRun();
    }

    static <A, X extends Exception> IO<A> delay(CheckedSupplier<? extends A, X> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return () -> Objects.requireNonNull(supplier.get(), "supplier result");
    }

    static <A> IO<A> pure(A value) {
        Objects.requireNonNull(value, "value");
        return () -> value;
    }

    static <A> IO<A> failed(Throwable error) {
        Objects.requireNonNull(error, "error");
        return () -> {
            throw toException(error);
        };
    }

    static IO<Unit> exec(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return () -> {
            runnable.run();
            return Unit.INSTANCE;
        };
    }

    static IO<Unit> fromRunnable(Runnable runnable) {
        return exec(runnable);
    }

    static IO<Unit> unit() {
        return pure(Unit.INSTANCE);
    }

    static <R, A> IO<A> bracket(
            IO<R> acquire,
            Function<? super R, ? extends IO<A>> use,
            Function<? super R, IO<Unit>> release) {
        return IOResource.of(acquire, release).use(resource -> Objects.requireNonNull(use.apply(resource), "use result"));
    }

    static <A> IO<A> unbox(App<Mu, A> value) {
        return (IO<A>) Validation.kind().narrowWithTypeCheck(value, IO.class);
    }

    static Applicative<IO.Mu, IOMonad.Mu> applicative() {
        return IOMonad.INSTANCE;
    }

    static Monad<IO.Mu, IOMonad.Mu> monad() {
        return IOMonad.INSTANCE;
    }

    static MonadError<IO.Mu, Throwable, IOMonad.Mu> monadError() {
        return IOMonad.INSTANCE;
    }

    static Selective<IO.Mu, IOMonad.Mu> selective() {
        return IOMonad.INSTANCE;
    }

    default <B> IO<B> map(Function<? super A, ? extends B> f) {
        Validation.function().require(f, "f", MAP);
        return () -> Validation.function().requireNonNullResult(f.apply(unsafeRun()), "f", MAP);
    }

    default <B> IO<B> flatMap(Function<? super A, ? extends IO<B>> f) {
        Validation.function().require(f, "f", FLAT_MAP);
        return new FlatMappedIO<>(this, f);
    }

    default <B> IO<B> then(Supplier<IO<B>> next) {
        Objects.requireNonNull(next, "next");
        return flatMap(ignored -> Objects.requireNonNull(next.get(), "next IO"));
    }

    default IO<A> peek(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    default IO<A> recover(Function<? super Throwable, ? extends A> f) {
        Validation.function().require(f, "f", RECOVER);
        return () -> {
            try {
                return unsafeRun();
            } catch (Throwable error) {
                return Validation.function().requireNonNullResult(f.apply(error), "f", RECOVER);
            }
        };
    }

    default IO<A> recoverWith(Function<? super Throwable, IO<A>> f) {
        Validation.function().require(f, "f", RECOVER_WITH);
        return () -> {
            try {
                return unsafeRun();
            } catch (Throwable error) {
                return Validation.function().requireNonNullResult(f.apply(error), "f", RECOVER_WITH).unsafeRun();
            }
        };
    }

    default IO<A> mapError(Function<? super Throwable, ? extends Throwable> f) {
        Validation.function().require(f, "f", MAP_ERROR);
        return () -> {
            try {
                return unsafeRun();
            } catch (Throwable error) {
                Throwable mapped = Validation.function().requireNonNullResult(f.apply(error), "f", MAP_ERROR);
                throw toException(mapped);
            }
        };
    }

    default IO<Either<Throwable, A>> attempt() {
        return () -> {
            try {
                return Either.right(unsafeRun());
            } catch (Throwable error) {
                return Either.left(error);
            }
        };
    }

    default IO<Unit> voided() {
        return map(ignored -> Unit.INSTANCE);
    }

    default IO<Unit> asUnit() {
        return voided();
    }

    default VTask<A> toVTask() {
        return VTask.delay(this::unsafeRun);
    }

    default IO<A> guarantee(IO<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return () -> {
            Throwable primary = null;
            try {
                return unsafeRun();
            } catch (Throwable error) {
                primary = error;
                throw toException(error);
            } finally {
                try {
                    finalizer.unsafeRun();
                } catch (Throwable finalizerError) {
                    if (primary != null) {
                        primary.addSuppressed(finalizerError);
                    } else {
                        throw toException(finalizerError);
                    }
                }
            }
        };
    }

    private static Exception toException(Throwable error) {
        if (error instanceof Exception exception) {
            return exception;
        }
        if (error instanceof Error fatal) {
            throw fatal;
        }
        return new RuntimeException(error);
    }

    default IOResource<A> asResource(Function<? super A, IO<Unit>> release) {
        return IOResource.of(this, release);
    }

    default VTask<A> toVTask(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return VTask.async(() -> toVTask().unsafeRunAsync(executor));
    }

    default IO<A> retry(RetryPolicy policy) {
        return Retry.retryIO(this, policy);
    }

    default IO<A> circuitBreaker(CircuitBreaker circuitBreaker) {
        return circuitBreaker.protect(this);
    }

    default IO<A> bulkhead(Bulkhead bulkhead) {
        return bulkhead.protect(this);
    }

    final class FlatMappedIO<A, B> implements IO<B> {
        private final IO<A> source;
        private final Function<? super A, ? extends IO<B>> f;

        FlatMappedIO(IO<A> source, Function<? super A, ? extends IO<B>> f) {
            this.source = Objects.requireNonNull(source, "source");
            this.f = Objects.requireNonNull(f, "f");
        }

        @Override
        @SuppressWarnings("unchecked")
        public B unsafeRun() throws Exception {
            IO<?> current = this;
            Deque<Function<Object, IO<?>>> continuations = new ArrayDeque<>();

            while (true) {
                if (current instanceof FlatMappedIO<?, ?> fm) {
                    continuations.push((Function<Object, IO<?>>) fm.f);
                    current = fm.source;
                } else {
                    Object result = current.unsafeRun();
                    if (continuations.isEmpty()) {
                        return (B) result;
                    }
                    current = Validation.function().requireNonNullResult(
                            continuations.pop().apply(result),
                            "f",
                            FLAT_MAP);
                }
            }
        }
    }

    enum IOMonad implements MonadError<IO.Mu, Throwable, IOMonad.Mu>,
            Selective<IO.Mu, IOMonad.Mu> {
        INSTANCE;

        public static final class Mu implements MonadError.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<IO.Mu, A> of(A value) {
            return IO.pure(value);
        }

        @Override
        public <A, B> App<IO.Mu, B> flatMap(
                Function<? super A, ? extends App<IO.Mu, B>> f,
                App<IO.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            return IO.unbox(fa).flatMap(value ->
                    IO.unbox(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP)));
        }

        @Override
        public <A> App<IO.Mu, A> raiseError(Throwable error) {
            return IO.failed(error);
        }

        @Override
        public <A> App<IO.Mu, A> handleErrorWith(
                App<IO.Mu, A> value,
                Function<? super Throwable, ? extends App<IO.Mu, A>> handler) {
            Validation.function().validateHandleErrorWith(value, handler);
            return IO.unbox(value).recoverWith(error ->
                    IO.unbox(Validation.function().requireNonNullResult(handler.apply(error), "handler", HANDLE_ERROR_WITH)));
        }

        @Override
        public <A, B> App<IO.Mu, B> select(
                App<IO.Mu, Either<A, B>> value,
                App<IO.Mu, ? extends Function<A, B>> function) {
            return IO.unbox(value).flatMap(inner -> {
                Either<A, B> either = Validation.coreType().requireValue(inner, "select value", IO.class, SELECT);
                if (either.isRight()) {
                    return IO.pure(either.right());
                }
                return IO.unbox(function).map(fn -> Validation.coreType()
                        .requireValue(fn, "select function", IO.class, SELECT)
                        .apply(either.left()));
            });
        }

        @Override
        public <A> App<IO.Mu, A> ifS(
                App<IO.Mu, Boolean> condition,
                Supplier<? extends App<IO.Mu, A>> thenValue,
                Supplier<? extends App<IO.Mu, A>> elseValue) {
            Objects.requireNonNull(thenValue, "thenValue");
            Objects.requireNonNull(elseValue, "elseValue");
            return IO.unbox(condition).flatMap(test -> {
                Supplier<? extends App<IO.Mu, A>> branch = Boolean.TRUE.equals(test) ? thenValue : elseValue;
                return IO.unbox(Validation.function().requireNonNullResult(branch.get(), "branch", IF_S));
            });
        }
    }
}

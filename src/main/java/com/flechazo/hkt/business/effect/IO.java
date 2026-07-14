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

import static com.flechazo.hkt.util.validation.Operation.*;

/**
 * Represents a deferred synchronous effect that produces a non-null result.
 *
 * @param <A> the result type
 */
@FunctionalInterface
public interface IO<A> extends App<IO.Mu, A> {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    /**
     * Executes this computation.
     *
     * @return the computed result
     * @throws Exception if the computation fails with an exception
     */
    A unsafeRun() throws Exception;

    /**
     * Executes this computation on the calling thread.
     *
     * @return the computed result
     * @throws Exception if the computation fails with an exception
     */
    default A unsafeRunSync() throws Exception {
        return unsafeRun();
    }

    /**
     * Creates a deferred computation from a checked supplier.
     *
     * @param <A> the result type
     * @param <X> the checked exception type
     * @param supplier the computation evaluated on execution
     * @return the deferred computation
     */
    static <A, X extends Exception> IO<A> delay(CheckedSupplier<? extends A, X> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return () -> Objects.requireNonNull(supplier.get(), "supplier result");
    }

    /**
     * Creates a computation that returns a value.
     *
     * @param <A> the result type
     * @param value the result value
     * @return a successful computation
     */
    static <A> IO<A> pure(A value) {
        Objects.requireNonNull(value, "value");
        return () -> value;
    }

    /**
     * Creates a computation that fails with a cause.
     *
     * @param <A> the result type
     * @param error the failure cause
     * @return a failed computation
     */
    static <A> IO<A> failed(Throwable error) {
        Objects.requireNonNull(error, "error");
        return () -> {
            throw toException(error);
        };
    }

    /**
     * Creates a computation that executes an action and returns {@link Unit}.
     *
     * @param runnable the action evaluated on execution
     * @return the deferred action
     */
    static IO<Unit> exec(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return () -> {
            runnable.run();
            return Unit.INSTANCE;
        };
    }

    /**
     * Creates a computation that executes an action and returns {@link Unit}.
     *
     * @param runnable the action evaluated on execution
     * @return the deferred action
     */
    static IO<Unit> fromRunnable(Runnable runnable) {
        return exec(runnable);
    }

    /**
     * Returns a computation producing {@link Unit}.
     *
     * @return the unit computation
     */
    static IO<Unit> unit() {
        return pure(Unit.INSTANCE);
    }

    /**
     * Acquires a resource, uses it, and releases it after use completes.
     *
     * @param <R> the resource type
     * @param <A> the result type
     * @param acquire the resource acquisition
     * @param use the computation selected from the acquired resource
     * @param release the release computation selected from the acquired resource
     * @return a computation producing the use result
     */
    static <R, A> IO<A> bracket(
            IO<R> acquire,
            Function<? super R, ? extends IO<A>> use,
            Function<? super R, IO<Unit>> release) {
        return IOResource.of(acquire, release).use(resource -> Objects.requireNonNull(use.apply(resource), "use result"));
    }

    /**
     * Narrows an encoded IO value.
     *
     * @param <A> the result type
     * @param value the encoded computation
     * @return the concrete IO computation
     * @throws com.flechazo.hkt.exception.KindUnwrapException if {@code value} is not an IO computation
     */
    static <A> IO<A> unbox(App<Mu, A> value) {
        return (IO<A>) Validation.kind().narrowWithTypeCheck(value, IO.class);
    }

    /**
     * Returns the IO applicative instance.
     *
     * @return the IO applicative
     */
    static Applicative<IO.Mu, IOMonad.Mu> applicative() {
        return IOMonad.INSTANCE;
    }

    /**
     * Returns the IO monad instance.
     *
     * @return the IO monad
     */
    static Monad<IO.Mu, IOMonad.Mu> monad() {
        return IOMonad.INSTANCE;
    }

    /**
     * Returns the IO monad-error instance.
     *
     * @return the IO monad-error instance
     */
    static MonadError<IO.Mu, Throwable, IOMonad.Mu> monadError() {
        return IOMonad.INSTANCE;
    }

    /**
     * Returns the IO selective instance.
     *
     * @return the IO selective
     */
    static Selective<IO.Mu, IOMonad.Mu> selective() {
        return IOMonad.INSTANCE;
    }

    /**
     * Transforms the successful result when this computation executes.
     *
     * @param <B> the transformed result type
     * @param f the result transformation
     * @return the transformed computation
     */
    default <B> IO<B> map(Function<? super A, ? extends B> f) {
        Validation.function().require(f, "f", MAP);
        return () -> Validation.function().requireNonNullResult(f.apply(unsafeRun()), "f", MAP);
    }

    /**
     * Sequences a computation selected from the successful result.
     *
     * @param <B> the next result type
     * @param f the function selecting the next computation
     * @return the sequenced computation
     */
    default <B> IO<B> flatMap(Function<? super A, ? extends IO<B>> f) {
        Validation.function().require(f, "f", FLAT_MAP);
        return new FlatMappedIO<>(this, f);
    }

    /**
     * Sequences a deferred computation and discards this result.
     *
     * @param <B> the next result type
     * @param next the supplier of the next computation
     * @return the sequenced computation
     */
    default <B> IO<B> then(Supplier<IO<B>> next) {
        Objects.requireNonNull(next, "next");
        return flatMap(ignored -> Objects.requireNonNull(next.get(), "next IO"));
    }

    /**
     * Observes a successful result while preserving it.
     *
     * @param action the operation invoked with the result
     * @return a computation preserving the original result
     */
    default IO<A> peek(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    /**
     * Recovers from a failure by producing a replacement value.
     *
     * @param f the function producing a value from the failure
     * @return a computation with recovery attached
     */
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

    /**
     * Recovers from a failure by selecting another computation.
     *
     * @param f the function selecting a replacement computation
     * @return a computation with recovery attached
     */
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

    /**
     * Transforms a failure cause.
     *
     * @param f the failure transformation
     * @return a computation with the transformed failure channel
     */
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

    /**
     * Captures execution failure in an explicit either value.
     *
     * @return a computation producing a left failure or right result
     */
    default IO<Either<Throwable, A>> attempt() {
        return () -> {
            try {
                return Either.right(unsafeRun());
            } catch (Throwable error) {
                return Either.left(error);
            }
        };
    }

    /**
     * Replaces the successful result with {@link Unit}.
     *
     * @return a computation producing {@link Unit}
     */
    default IO<Unit> voided() {
        return map(ignored -> Unit.INSTANCE);
    }

    /**
     * Replaces the successful result with {@link Unit}.
     *
     * @return a computation producing {@link Unit}
     */
    default IO<Unit> asUnit() {
        return voided();
    }

    /**
     * Converts this computation to a virtual-thread task.
     *
     * @return a task executing this IO computation
     */
    default VTask<A> toVTask() {
        return VTask.delay(this::unsafeRun);
    }

    /**
     * Registers a finalizer that executes after success or failure.
     *
     * <p>If both this computation and the finalizer fail, the finalizer failure is suppressed on the primary failure.
     *
     * @param finalizer the completion action
     * @return a computation with the finalizer attached
     */
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

    /**
     * Converts this computation to a managed resource.
     *
     * @param release the release computation selected from the acquired value
     * @return a managed resource that acquires by executing this computation
     */
    default IOResource<A> asResource(Function<? super A, IO<Unit>> release) {
        return IOResource.of(this, release);
    }

    /**
     * Converts this computation to a task scheduled by an executor.
     *
     * @param executor the executor used to start asynchronous execution
     * @return a task executing this computation through {@code executor}
     */
    default VTask<A> toVTask(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return VTask.async(() -> toVTask().unsafeRunAsync(executor));
    }

    /**
     * Retries failures according to a policy.
     *
     * @param policy the retry policy
     * @return a computation protected by the retry policy
     */
    default IO<A> retry(RetryPolicy policy) {
        return Retry.retryIO(this, policy);
    }

    /**
     * Protects execution with a circuit breaker.
     *
     * @param circuitBreaker the circuit breaker
     * @return the protected computation
     */
    default IO<A> circuitBreaker(CircuitBreaker circuitBreaker) {
        return circuitBreaker.protect(this);
    }

    /**
     * Protects execution with a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @return the protected computation
     */
    default IO<A> bulkhead(Bulkhead bulkhead) {
        return bulkhead.protect(this);
    }

    /**
     * Represents a deferred sequence of two IO computations.
     *
     * @param <A> the first result type
     * @param <B> the final result type
     */
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

    /**
     * Provides IO monad-error and selective operations.
     */
    enum IOMonad implements MonadError<IO.Mu, Throwable, IOMonad.Mu>,
            Selective<IO.Mu, IOMonad.Mu> {
        /**
         * Provides the shared IO type-class instance.
         */
        INSTANCE;

        public static final class Mu implements MonadError.Mu {
            private Mu() {
            }
        }

        /**
         * Creates an encoded IO computation that returns a value.
         *
         * @param <A> the result type
         * @param value the result value
         * @return the successful computation in encoded form
         */
        @Override
        public <A> App<IO.Mu, A> of(A value) {
            return IO.pure(value);
        }

        /**
         * Sequences an encoded IO computation selected from the successful result.
         *
         * @param <A> the source result type
         * @param <B> the next result type
         * @param f the function selecting the next computation
         * @param fa the source computation
         * @return the sequenced computation in encoded form
         */
        @Override
        public <A, B> App<IO.Mu, B> flatMap(
                Function<? super A, ? extends App<IO.Mu, B>> f,
                App<IO.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            return IO.unbox(fa).flatMap(value ->
                    IO.unbox(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP)));
        }

        /**
         * Creates an encoded IO computation that fails with a cause.
         *
         * @param <A> the result type
         * @param error the failure cause
         * @return the failed computation in encoded form
         */
        @Override
        public <A> App<IO.Mu, A> raiseError(Throwable error) {
            return IO.failed(error);
        }

        /**
         * Recovers an encoded IO failure by selecting another encoded computation.
         *
         * @param <A> the result type
         * @param value the source computation
         * @param handler the function selecting a replacement computation from the failure
         * @return the recovered computation in encoded form
         */
        @Override
        public <A> App<IO.Mu, A> handleErrorWith(
                App<IO.Mu, A> value,
                Function<? super Throwable, ? extends App<IO.Mu, A>> handler) {
            Validation.function().validateHandleErrorWith(value, handler);
            return IO.unbox(value).recoverWith(error ->
                    IO.unbox(Validation.function().requireNonNullResult(handler.apply(error), "handler", HANDLE_ERROR_WITH)));
        }

        /**
         * Resolves an encoded either result and evaluates the function computation only for a left value.
         *
         * @param <A> the function argument type
         * @param <B> the result type
         * @param value the computation producing the branch value
         * @param function the computation producing the function for a left branch
         * @return the selected computation in encoded form
         */
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

        /**
         * Evaluates one deferred IO branch according to an effectful condition.
         *
         * @param <A> the result type
         * @param condition the computation producing the condition
         * @param thenValue the deferred computation used for a true condition
         * @param elseValue the deferred computation used for a false condition
         * @return the selected computation in encoded form
         */
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

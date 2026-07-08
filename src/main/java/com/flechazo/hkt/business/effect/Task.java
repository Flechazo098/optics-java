package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.resilience.Bulkhead;
import com.flechazo.hkt.business.resilience.CircuitBreaker;
import com.flechazo.hkt.business.resilience.Resilience;
import com.flechazo.hkt.business.resilience.ResilienceBuilder;
import com.flechazo.hkt.business.resilience.Retry;
import com.flechazo.hkt.business.resilience.RetryPolicy;
import com.flechazo.hkt.util.validation.Validation;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BiFunction;
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
public interface Task<A> extends App<Task.Mu, A> {
    A execute() throws Throwable;

    final class Mu implements K1 {
        private Mu() {
        }
    }

    static <A> Task<A> of(Callable<? extends A> callable) {
        Objects.requireNonNull(callable, "callable");
        return callable::call;
    }

    static <A> Task<A> delay(CheckedSupplier<? extends A, ?> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return supplier::get;
    }

    static <A> Task<A> blocking(Callable<? extends A> callable) {
        return of(callable);
    }

    static <A> Task<A> pure(A value) {
        Objects.requireNonNull(value, "value");
        return () -> value;
    }

    static <A> Task<A> succeed(A value) {
        return pure(value);
    }

    static <A> Task<A> failed(Throwable error) {
        Objects.requireNonNull(error, "error");
        return () -> {
            throw error;
        };
    }

    static <A> Task<A> fail(Throwable error) {
        return failed(error);
    }

    static Task<Unit> exec(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return () -> {
            runnable.run();
            return Unit.INSTANCE;
        };
    }

    static <A> Task<A> async(Supplier<? extends CompletableFuture<A>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return () -> unwrapFuture(supplier.get());
    }

    static <A> Task<A> fromFuture(CompletableFuture<A> future) {
        Objects.requireNonNull(future, "future");
        return () -> unwrapFuture(future);
    }

    static <A> Task<A> fromFuture(Supplier<? extends CompletableFuture<A>> future) {
        return async(future);
    }

    static Task<Unit> unit() {
        return pure(Unit.INSTANCE);
    }

    static <R, A> Task<A> bracket(
            Task<R> acquire,
            Function<? super R, ? extends Task<A>> use,
            Function<? super R, Task<Unit>> release) {
        return Resource.of(acquire, release).use(resource -> Objects.requireNonNull(use.apply(resource), "use result"));
    }

    static <A> Task<A> unbox(App<Mu, A> value) {
        return (Task<A>) Validation.kind().narrowWithTypeCheck(value, Task.class);
    }

    static Applicative<Task.Mu, TaskMonad.Mu> applicative() {
        return TaskMonad.INSTANCE;
    }

    static Monad<Task.Mu, TaskMonad.Mu> monad() {
        return TaskMonad.INSTANCE;
    }

    static MonadError<Task.Mu, Throwable, TaskMonad.Mu> monadError() {
        return TaskMonad.INSTANCE;
    }

    static Selective<Task.Mu, TaskMonad.Mu> selective() {
        return TaskMonad.INSTANCE;
    }

    default Callable<A> asCallable() {
        return () -> {
            try {
                return execute();
            } catch (Exception exception) {
                throw exception;
            } catch (Throwable throwable) {
                if (throwable instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException(throwable);
            }
        };
    }

    default A unsafeRun() {
        try {
            return execute();
        } catch (RuntimeException | Error error) {
            throw error;
        } catch (Throwable throwable) {
            throw new TaskExecutionException(throwable);
        }
    }

    default A run() {
        return unsafeRun();
    }

    default Try<A> runSafe() {
        try {
            return Try.success(execute());
        } catch (Throwable throwable) {
            return Try.failure(throwable);
        }
    }

    default CompletableFuture<A> runAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        }, command -> Thread.ofVirtual().start(command));
    }

    default CompletableFuture<A> unsafeRunAsync() {
        return runAsync();
    }

    default CompletableFuture<A> unsafeRunAsync(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        }, executor);
    }

    default <B> Task<B> map(Function<? super A, ? extends B> f) {
        Validation.function().require(f, "f", MAP);
        return () -> Validation.function().requireNonNullResult(f.apply(execute()), "f", MAP);
    }

    default <B> Task<B> flatMap(Function<? super A, ? extends Task<B>> f) {
        Validation.function().require(f, "f", FLAT_MAP);
        return () -> Validation.function().requireNonNullResult(f.apply(execute()), "f", FLAT_MAP).execute();
    }

    default <B> Task<B> via(Function<? super A, ? extends Task<B>> f) {
        return flatMap(f);
    }

    default <B, C> Task<C> zipWith(Task<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combiner, "combiner");
        return parZipWith(other, combiner);
    }

    default <B, C> Task<C> parZipWith(Task<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combiner, "combiner");
        return Par.map2(this, other, combiner);
    }

    default Task<A> race(Task<A> other) {
        Objects.requireNonNull(other, "other");
        return Par.race(List.of(this, other));
    }

    default <B> Task<B> then(Supplier<Task<B>> next) {
        Objects.requireNonNull(next, "next");
        return flatMap(ignored -> Objects.requireNonNull(next.get(), "next task"));
    }

    default Task<Unit> voided() {
        return map(ignored -> Unit.INSTANCE);
    }

    default Task<Unit> asUnit() {
        return voided();
    }

    default Task<A> recover(Function<? super Throwable, ? extends A> f) {
        Validation.function().require(f, "f", RECOVER);
        return () -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                return Validation.function().requireNonNullResult(f.apply(throwable), "f", RECOVER);
            }
        };
    }

    default Task<A> recoverWith(Function<? super Throwable, ? extends Task<A>> f) {
        Validation.function().require(f, "f", RECOVER_WITH);
        return () -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                return Validation.function().requireNonNullResult(f.apply(throwable), "f", RECOVER_WITH).execute();
            }
        };
    }

    default Task<A> mapError(Function<? super Throwable, ? extends Throwable> f) {
        Validation.function().require(f, "f", MAP_ERROR);
        return () -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                throw Validation.function().requireNonNullResult(f.apply(throwable), "f", MAP_ERROR);
            }
        };
    }

    default Task<A> peek(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    default Task<A> peekFailure(Consumer<? super Throwable> action) {
        Objects.requireNonNull(action, "action");
        return () -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                action.accept(throwable);
                throw throwable;
            }
        };
    }

    default Task<Either<Throwable, A>> attempt() {
        return () -> {
            try {
                return Either.right(execute());
            } catch (Throwable throwable) {
                return Either.left(throwable);
            }
        };
    }

    default Task<A> timeout(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return () -> {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<A> future = executor.submit(asCallable());
                return future.get(duration.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException exception) {
                throw unwrap(exception);
            }
        };
    }

    default Task<A> onExecutor(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return () -> unwrapFuture(unsafeRunAsync(executor));
    }

    default Task<A> guarantee(Task<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return () -> {
            Throwable primary = null;
            try {
                return execute();
            } catch (Throwable error) {
                primary = error;
                throw error;
            } finally {
                try {
                    finalizer.execute();
                } catch (Throwable finalizerError) {
                    if (primary != null) {
                        primary.addSuppressed(finalizerError);
                    } else {
                        throw finalizerError;
                    }
                }
            }
        };
    }

    default <B> Task<B> use(Resource<A> resource, Function<? super A, Task<B>> use) {
        return resource.use(use);
    }

    default Resource<A> asResource(Function<? super A, Task<Unit>> release) {
        return Resource.of(this, release);
    }

    default VIO<A> toVIO() {
        return () -> {
            try {
                return execute();
            } catch (Exception exception) {
                throw exception;
            } catch (Throwable throwable) {
                if (throwable instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException(throwable);
            }
        };
    }

    default Task<A> retry(RetryPolicy policy) {
        return Retry.retryTask(this, policy);
    }

    default Task<A> circuitBreaker(CircuitBreaker circuitBreaker) {
        return circuitBreaker.protect(this);
    }

    default Task<A> bulkhead(Bulkhead bulkhead) {
        return bulkhead.protect(this);
    }

    default ResilienceBuilder<A> resilient() {
        return Resilience.builder(this);
    }

    private static <A> A unwrapFuture(CompletableFuture<? extends A> future) throws Throwable {
        Objects.requireNonNull(future, "future");
        try {
            return future.get();
        } catch (ExecutionException exception) {
            throw unwrap(exception);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    enum TaskMonad implements MonadError<Task.Mu, Throwable, TaskMonad.Mu>,
            Selective<Task.Mu, TaskMonad.Mu> {
        INSTANCE;

        public static final class Mu implements MonadError.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<Task.Mu, A> of(A value) {
            return Task.pure(value);
        }

        @Override
        public <A, B> App<Task.Mu, B> flatMap(
                Function<? super A, ? extends App<Task.Mu, B>> f,
                App<Task.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            return Task.unbox(fa).flatMap(value ->
                    Task.unbox(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP)));
        }

        @Override
        public <A> App<Task.Mu, A> raiseError(Throwable error) {
            return Task.failed(error);
        }

        @Override
        public <A> App<Task.Mu, A> handleErrorWith(
                App<Task.Mu, A> value,
                Function<? super Throwable, ? extends App<Task.Mu, A>> handler) {
            Validation.function().validateHandleErrorWith(value, handler);
            return Task.unbox(value).recoverWith(error ->
                    Task.unbox(Validation.function().requireNonNullResult(handler.apply(error), "handler", HANDLE_ERROR_WITH)));
        }

        @Override
        public <A, B> App<Task.Mu, B> select(
                App<Task.Mu, Either<A, B>> value,
                App<Task.Mu, ? extends Function<A, B>> function) {
            return Task.unbox(value).flatMap(inner -> {
                Either<A, B> either = Validation.coreType().requireValue(inner, "select value", Task.class, SELECT);
                if (either.isRight()) {
                    return Task.pure(either.right());
                }
                return Task.unbox(function).map(fn -> Validation.coreType()
                        .requireValue(fn, "select function", Task.class, SELECT)
                        .apply(either.left()));
            });
        }

        @Override
        public <A> App<Task.Mu, A> ifS(
                App<Task.Mu, Boolean> condition,
                Supplier<? extends App<Task.Mu, A>> thenValue,
                Supplier<? extends App<Task.Mu, A>> elseValue) {
            Objects.requireNonNull(thenValue, "thenValue");
            Objects.requireNonNull(elseValue, "elseValue");
            return Task.unbox(condition).flatMap(test -> {
                Supplier<? extends App<Task.Mu, A>> branch = Boolean.TRUE.equals(test) ? thenValue : elseValue;
                return Task.unbox(Validation.function().requireNonNullResult(branch.get(), "branch", IF_S));
            });
        }
    }
}

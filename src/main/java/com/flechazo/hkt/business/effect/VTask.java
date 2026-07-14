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
import java.util.ArrayDeque;
import java.util.Deque;
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
public interface VTask<A> extends App<VTask.Mu, A> {
    A execute() throws Throwable;

    final class Mu implements K1 {
        private Mu() {
        }
    }

    static <A> VTask<A> of(Callable<? extends A> callable) {
        Objects.requireNonNull(callable, "callable");
        return callable::call;
    }

    static <A> VTask<A> delay(CheckedSupplier<? extends A, ?> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return supplier::get;
    }

    static <A> VTask<A> blocking(Callable<? extends A> callable) {
        return of(callable);
    }

    static <A> VTask<A> pure(A value) {
        Objects.requireNonNull(value, "value");
        return () -> value;
    }

    static <A> VTask<A> succeed(A value) {
        return pure(value);
    }

    static <A> VTask<A> failed(Throwable error) {
        Objects.requireNonNull(error, "error");
        return () -> {
            throw error;
        };
    }

    static <A> VTask<A> fail(Throwable error) {
        return failed(error);
    }

    static VTask<Unit> exec(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return () -> {
            runnable.run();
            return Unit.INSTANCE;
        };
    }

    static <A> VTask<A> async(Supplier<? extends CompletableFuture<A>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return () -> unwrapFuture(supplier.get());
    }

    static <A> VTask<A> fromFuture(CompletableFuture<A> future) {
        Objects.requireNonNull(future, "future");
        return () -> unwrapFuture(future);
    }

    static <A> VTask<A> fromFuture(Supplier<? extends CompletableFuture<A>> future) {
        return async(future);
    }

    static VTask<Unit> unit() {
        return pure(Unit.INSTANCE);
    }

    static <R, A> VTask<A> bracket(
            VTask<R> acquire,
            Function<? super R, ? extends VTask<A>> use,
            Function<? super R, VTask<Unit>> release) {
        return Resource.of(acquire, release).use(resource -> Objects.requireNonNull(use.apply(resource), "use result"));
    }

    static <A> VTask<A> unbox(App<Mu, A> value) {
        return (VTask<A>) Validation.kind().narrowWithTypeCheck(value, VTask.class);
    }

    static Applicative<VTask.Mu, VTaskMonad.Mu> applicative() {
        return VTaskMonad.INSTANCE;
    }

    static Monad<VTask.Mu, VTaskMonad.Mu> monad() {
        return VTaskMonad.INSTANCE;
    }

    static MonadError<VTask.Mu, Throwable, VTaskMonad.Mu> monadError() {
        return VTaskMonad.INSTANCE;
    }

    static Selective<VTask.Mu, VTaskMonad.Mu> selective() {
        return VTaskMonad.INSTANCE;
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
            throw new VTaskExecutionException(throwable);
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

    default <B> VTask<B> map(Function<? super A, ? extends B> f) {
        Validation.function().require(f, "f", MAP);
        return flatMap(value -> VTask.pure(
                Validation.function().requireNonNullResult(f.apply(value), "f", MAP)));
    }

    default <B> VTask<B> flatMap(Function<? super A, ? extends VTask<B>> f) {
        Validation.function().require(f, "f", FLAT_MAP);
        return new FlatMappedVTask<>(this, f);
    }

    default <B> VTask<B> via(Function<? super A, ? extends VTask<B>> f) {
        return flatMap(f);
    }

    default <B, C> VTask<C> zipWith(VTask<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combiner, "combiner");
        return parZipWith(other, combiner);
    }

    default <B, C> VTask<C> parZipWith(VTask<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combiner, "combiner");
        return () -> {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<A> left = executor.submit(asCallable());
                Future<B> right = executor.submit(other.asCallable());
                return Objects.requireNonNull(combiner.apply(left.get(), right.get()), "parZipWith result");
            } catch (ExecutionException exception) {
                throw unwrap(exception);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            }
        };
    }

    default <B> VTask<B> then(Supplier<VTask<B>> next) {
        Objects.requireNonNull(next, "next");
        return flatMap(ignored -> Objects.requireNonNull(next.get(), "next task"));
    }

    default VTask<Unit> voided() {
        return map(ignored -> Unit.INSTANCE);
    }

    default VTask<Unit> asUnit() {
        return voided();
    }

    default VTask<A> recover(Function<? super Throwable, ? extends A> f) {
        Validation.function().require(f, "f", RECOVER);
        return () -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                return Validation.function().requireNonNullResult(f.apply(throwable), "f", RECOVER);
            }
        };
    }

    default VTask<A> recoverWith(Function<? super Throwable, ? extends VTask<A>> f) {
        Validation.function().require(f, "f", RECOVER_WITH);
        return () -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                return Validation.function().requireNonNullResult(f.apply(throwable), "f", RECOVER_WITH).execute();
            }
        };
    }

    default VTask<A> mapError(Function<? super Throwable, ? extends Throwable> f) {
        Validation.function().require(f, "f", MAP_ERROR);
        return () -> {
            try {
                return execute();
            } catch (Throwable throwable) {
                throw Validation.function().requireNonNullResult(f.apply(throwable), "f", MAP_ERROR);
            }
        };
    }

    default VTask<A> peek(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    default VTask<A> peekFailure(Consumer<? super Throwable> action) {
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

    default VTask<Either<Throwable, A>> attempt() {
        return () -> {
            try {
                return Either.right(execute());
            } catch (Throwable throwable) {
                return Either.left(throwable);
            }
        };
    }

    default VTask<A> timeout(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return () -> {
            // No try-with-resources: ExecutorService.close() awaits task termination,
            // which would delay the timeout until the underlying task completes.
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            try {
                Future<A> future = executor.submit(asCallable());
                return future.get(duration.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException exception) {
                throw unwrap(exception);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } finally {
                executor.shutdownNow();
            }
        };
    }

    default VTask<A> onExecutor(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return () -> unwrapFuture(unsafeRunAsync(executor));
    }

    default VTask<A> guarantee(VTask<Unit> finalizer) {
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

    default <B> VTask<B> use(Resource<A> resource, Function<? super A, VTask<B>> use) {
        return resource.use(use);
    }

    default Resource<A> asResource(Function<? super A, VTask<Unit>> release) {
        return Resource.of(this, release);
    }

    default IO<A> toIO() {
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

    default VTask<A> retry(RetryPolicy policy) {
        return Retry.retryVTask(this, policy);
    }

    default VTask<A> circuitBreaker(CircuitBreaker circuitBreaker) {
        return circuitBreaker.protect(this);
    }

    default VTask<A> bulkhead(Bulkhead bulkhead) {
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

    final class FlatMappedVTask<A, B> implements VTask<B> {
        private final VTask<A> source;
        private final Function<? super A, ? extends VTask<B>> function;

        FlatMappedVTask(VTask<A> source, Function<? super A, ? extends VTask<B>> function) {
            this.source = Objects.requireNonNull(source, "source");
            this.function = Objects.requireNonNull(function, "function");
        }

        @Override
        @SuppressWarnings("unchecked")
        public B execute() throws Throwable {
            VTask<?> current = this;
            Deque<Function<Object, VTask<?>>> continuations = new ArrayDeque<>();

            while (true) {
                if (current instanceof FlatMappedVTask<?, ?> flatMapped) {
                    continuations.push((Function<Object, VTask<?>>) flatMapped.function);
                    current = flatMapped.source;
                    continue;
                }

                Object result = current.execute();
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

    enum VTaskMonad implements MonadError<VTask.Mu, Throwable, VTaskMonad.Mu>,
            Selective<VTask.Mu, VTaskMonad.Mu> {
        INSTANCE;

        public static final class Mu implements MonadError.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<VTask.Mu, A> of(A value) {
            return VTask.pure(value);
        }

        @Override
        public <A, B> App<VTask.Mu, B> flatMap(
                Function<? super A, ? extends App<VTask.Mu, B>> f,
                App<VTask.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            return VTask.unbox(fa).flatMap(value ->
                    VTask.unbox(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP)));
        }

        @Override
        public <A> App<VTask.Mu, A> raiseError(Throwable error) {
            return VTask.failed(error);
        }

        @Override
        public <A> App<VTask.Mu, A> handleErrorWith(
                App<VTask.Mu, A> value,
                Function<? super Throwable, ? extends App<VTask.Mu, A>> handler) {
            Validation.function().validateHandleErrorWith(value, handler);
            return VTask.unbox(value).recoverWith(error ->
                    VTask.unbox(Validation.function().requireNonNullResult(handler.apply(error), "handler", HANDLE_ERROR_WITH)));
        }

        @Override
        public <A, B> App<VTask.Mu, B> select(
                App<VTask.Mu, Either<A, B>> value,
                App<VTask.Mu, ? extends Function<A, B>> function) {
            return VTask.unbox(value).flatMap(inner -> {
                Either<A, B> either = Validation.coreType().requireValue(inner, "select value", VTask.class, SELECT);
                if (either.isRight()) {
                    return VTask.pure(either.right());
                }
                return VTask.unbox(function).map(fn -> Validation.coreType()
                        .requireValue(fn, "select function", VTask.class, SELECT)
                        .apply(either.left()));
            });
        }

        @Override
        public <A> App<VTask.Mu, A> ifS(
                App<VTask.Mu, Boolean> condition,
                Supplier<? extends App<VTask.Mu, A>> thenValue,
                Supplier<? extends App<VTask.Mu, A>> elseValue) {
            Objects.requireNonNull(thenValue, "thenValue");
            Objects.requireNonNull(elseValue, "elseValue");
            return VTask.unbox(condition).flatMap(test -> {
                Supplier<? extends App<VTask.Mu, A>> branch = Boolean.TRUE.equals(test) ? thenValue : elseValue;
                return VTask.unbox(Validation.function().requireNonNullResult(branch.get(), "branch", IF_S));
            });
        }
    }
}

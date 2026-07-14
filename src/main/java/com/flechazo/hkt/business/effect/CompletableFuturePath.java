package com.flechazo.hkt.business.effect;


import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Recoverable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.FutureCombinable;
import com.flechazo.hkt.business.control.EitherPath;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.control.TryPath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.resilience.RetryPolicy;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

public final class CompletableFuturePath<A>
        implements Recoverable<Throwable, A>, FutureCombinable<A> {
    private final CompletableFuture<A> value;

    private CompletableFuturePath(CompletableFuture<A> value) {
        this.value = value;
    }

    public static <A> CompletableFuturePath<A> fromFuture(CompletableFuture<A> future) {
        return new CompletableFuturePath<>(future);
    }

    public static <A> CompletableFuturePath<A> completed(A value) {
        return new CompletableFuturePath<>(CompletableFuture.completedFuture(value));
    }

    public static <A> CompletableFuturePath<A> failed(Throwable error) {
        return new CompletableFuturePath<>(CompletableFuture.failedFuture(error));
    }

    public static <A> CompletableFuturePath<A> supplyAsync(Supplier<A> supplier) {
        return new CompletableFuturePath<>(CompletableFuture.supplyAsync(supplier));
    }

    public static <A> CompletableFuturePath<A> supplyAsync(Supplier<A> supplier, Executor executor) {
        return new CompletableFuturePath<>(CompletableFuture.supplyAsync(supplier, executor));
    }

    public static <A> CompletableFuturePath<A> supplyAsyncWithRetry(Supplier<A> supplier, RetryPolicy policy) {
        return new CompletableFuturePath<>(VTask.delay(supplier::get).retry(policy).runAsync());
    }

    public static <A> CompletableFuturePath<A> supplyAsyncWithRetry(
            Supplier<A> supplier,
            int maxRetries,
            Duration initialDelay) {
        return supplyAsyncWithRetry(supplier, RetryPolicy.fixedDelay(maxRetries, initialDelay));
    }

    public CompletableFuture<A> run() {
        return value;
    }

    public CompletableFuture<A> toCompletableFuture() {
        return value;
    }

    public A join() {
        return value.join();
    }

    public A join(Duration timeout) throws TimeoutException {
        try {
            return value.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            throw timeoutException;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new CompletionException(interrupted);
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    @Override
    public <B> CompletableFuturePath<B> map(Function<? super A, ? extends B> mapper) {
        return new CompletableFuturePath<>(value.thenApply(mapper));
    }

    @Override
    public CompletableFuturePath<A> peek(Consumer<? super A> consumer) {
        return map(result -> {
            consumer.accept(result);
            return result;
        });
    }

    /**
     * Combines the completed values of two future paths.
     *
     * @param <B> the other result type
     * @param <C> the combined result type
     * @param other the future path to combine with this path
     * @param combiner the function combining both results
     * @return the combined future path
     * @throws IllegalArgumentException if {@code other} is not a future path
     */
    @Override
    public <B, C> CompletableFuturePath<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof CompletableFuturePath<?> otherFuture)) {
            throw new IllegalArgumentException("Cannot zipWith non-CompletableFuturePath: " + other.getClass());
        }
        return new CompletableFuturePath<>(value.thenCombine(((CompletableFuturePath<B>) otherFuture).value, combiner));
    }

    @Override
    public <B> CompletableFuturePath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        return new CompletableFuturePath<>(value.thenCompose(result -> {
            Chainable<B> mapped = mapper.apply(result);
            if (!(mapped instanceof CompletableFuturePath<?> futurePath)) {
                throw new IllegalArgumentException("via mapper must return CompletableFuturePath, got: " + mapped.getClass());
            }
            return ((CompletableFuturePath<B>) futurePath).value;
        }));
    }

    @Override
    public <B> CompletableFuturePath<B> then(Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    @Override
    public CompletableFuturePath<A> recover(Function<? super Throwable, ? extends A> recovery) {
        return new CompletableFuturePath<>(value.exceptionally(error -> recovery.apply(unwrap(error))));
    }

    @Override
    public CompletableFuturePath<A> recoverWith(Function<? super Throwable, ? extends Recoverable<Throwable, A>> recovery) {
        return new CompletableFuturePath<>(value.handle((result, error) -> {
            if (error == null) {
                return CompletableFuture.completedFuture(result);
            }
            Recoverable<Throwable, A> recovered = recovery.apply(unwrap(error));
            if (!(recovered instanceof CompletableFuturePath<?> futurePath)) {
                throw new IllegalArgumentException("recovery must return CompletableFuturePath, got: " + recovered.getClass());
            }
            return ((CompletableFuturePath<A>) futurePath).value;
        }).thenCompose(Function.identity()));
    }

    @Override
    public CompletableFuturePath<A> orElse(Supplier<? extends Recoverable<Throwable, A>> alternative) {
        return recoverWith(ignored -> alternative.get());
    }

    @Override
    public <E2> Recoverable<E2, A> mapError(Function<? super Throwable, ? extends E2> mapper) {
        return new EitherPath<>(toEitherPath().run().mapLeft(mapper));
    }

    public CompletableFuturePath<A> withTimeout(Duration timeout) {
        return new CompletableFuturePath<>(value.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS));
    }

    public CompletableFuturePath<A> completeOnTimeout(A defaultValue, Duration timeout) {
        return new CompletableFuturePath<>(value.completeOnTimeout(defaultValue, timeout.toMillis(), TimeUnit.MILLISECONDS));
    }

    public CompletableFuturePath<A> onExecutor(Executor executor) {
        return new CompletableFuturePath<>(value.thenApplyAsync(Function.identity(), executor));
    }

    public <B, C> CompletableFuturePath<C> parZipWith(
            CompletableFuturePath<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        return new CompletableFuturePath<>(value.thenCombine(other.value, combiner));
    }

    public CompletableFuturePath<A> race(CompletableFuturePath<A> other) {
        CompletableFuture<A> result = new CompletableFuture<>();
        AtomicInteger failureCount = new AtomicInteger();
        AtomicReference<@Nullable Throwable> lastFailure = new AtomicReference<>();
        BiConsumer<A, Throwable> handler = (success, error) -> {
            if (error == null) {
                result.complete(success);
                return;
            }
            lastFailure.set(unwrap(error));
            if (failureCount.incrementAndGet() == 2) {
                result.completeExceptionally(lastFailure.get());
            }
        };
        value.whenComplete(handler);
        other.value.whenComplete(handler);
        return new CompletableFuturePath<>(result);
    }

    public CompletableFuturePath<A> withRetry(RetryPolicy policy) {
        return new CompletableFuturePath<>(VTask.fromFuture(value).retry(policy).runAsync());
    }

    public CompletableFuturePath<A> retry(int maxAttempts, Duration initialDelay) {
        return withRetry(RetryPolicy.fixed(maxAttempts, initialDelay));
    }

    public IOPath<A> toIOPath() {
        return Pathway.io(value::join);
    }

    public TryPath<A> toTryPath() {
        try {
            return Pathway.success(value.join());
        } catch (Throwable error) {
            return Pathway.failure(unwrap(error));
        }
    }

    public EitherPath<Throwable, A> toEitherPath() {
        try {
            return Pathway.right(value.join());
        } catch (Throwable error) {
            return Pathway.left(unwrap(error));
        }
    }

    public MaybePath<A> toMaybePath() {
        return toEitherPath().toMaybePath();
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

}

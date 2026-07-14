package com.flechazo.hkt.business.effect;


import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.Effectful;
import com.flechazo.hkt.business.capability.combinable.VTaskCombinable;
import com.flechazo.hkt.business.control.TryPath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.resilience.Bulkhead;
import com.flechazo.hkt.business.resilience.CircuitBreaker;
import com.flechazo.hkt.business.resilience.Resilience;
import com.flechazo.hkt.business.resilience.ResilienceBuilder;
import com.flechazo.hkt.business.resilience.RetryPolicy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class VTaskPath<A> implements Effectful<A>, VTaskCombinable<A> {
    private final VTask<A> value;

    public VTaskPath(VTask<A> value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public VTask<A> run() {
        return value;
    }

    @Override
    public A unsafeRun() {
        return value.unsafeRun();
    }

    public CompletableFuture<A> runAsync() {
        return value.runAsync();
    }

    public CompletableFuture<A> unsafeRunAsync(Executor executor) {
        return value.unsafeRunAsync(executor);
    }

    @Override
    public <B> VTaskPath<B> map(Function<? super A, ? extends B> mapper) {
        return new VTaskPath<>(value.map(mapper));
    }

    @Override
    public <B> VTaskPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new VTaskPath<>(value.flatMap(a -> {
            Chainable<B> mapped = mapper.apply(a);
            if (!(mapped instanceof VTaskPath<?> taskPath)) {
                throw new IllegalArgumentException("via mapper must return VTaskPath, got: " + mapped.getClass());
            }
            return ((VTaskPath<B>) taskPath).run();
        }));
    }

    @Override
    public <B, C> VTaskPath<C> zipWith(Combinable<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof VTaskPath<?> otherVTask)) {
            throw new IllegalArgumentException("Cannot zipWith non-VTaskPath: " + other.getClass());
        }
        return new VTaskPath<>(value.zipWith(((VTaskPath<B>) otherVTask).value, combiner));
    }

    public <B, C> VTaskPath<C> parZipWith(VTaskPath<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        return new VTaskPath<>(value.parZipWith(other.value, combiner));
    }

    @Override
    public <B> VTaskPath<B> then(Supplier<? extends Chainable<B>> next) {
        Objects.requireNonNull(next, "next");
        return via(ignored -> next.get());
    }

    public VTaskPath<A> recover(Function<? super Throwable, ? extends A> recovery) {
        return new VTaskPath<>(value.recover(recovery));
    }

    public VTaskPath<A> recoverWith(Function<? super Throwable, VTaskPath<A>> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        return new VTaskPath<>(value.recoverWith(error -> recovery.apply(error).run()));
    }

    public VTaskPath<A> handleError(Function<? super Throwable, ? extends A> recovery) {
        return recover(recovery);
    }

    public VTaskPath<A> handleErrorWith(Function<? super Throwable, ? extends Effectful<A>> recovery) {
        return new VTaskPath<>(value.recoverWith(error -> {
            Effectful<A> result = recovery.apply(error);
            if (result instanceof VTaskPath<?> taskPath) {
                return ((VTaskPath<A>) taskPath).run();
            }
            return VTask.delay(result::unsafeRun);
        }));
    }

    public VTaskPath<A> guarantee(Runnable finalizer) {
        return new VTaskPath<>(VTask.delay(() -> {
            try {
                return value.unsafeRun();
            } finally {
                finalizer.run();
            }
        }));
    }

    public VTaskPath<A> guarantee(VTaskPath<Unit> finalizer) {
        return new VTaskPath<>(value.guarantee(finalizer.run()));
    }

    public VTaskPath<A> mapError(Function<? super Throwable, ? extends Throwable> mapper) {
        return new VTaskPath<>(value.mapError(mapper));
    }

    public VTaskPath<A> peek(Consumer<? super A> action) {
        return new VTaskPath<>(value.peek(action));
    }

    public VTaskPath<A> peekFailure(Consumer<? super Throwable> action) {
        return new VTaskPath<>(value.peekFailure(action));
    }

    public VTaskPath<Either<Throwable, A>> attempt() {
        return new VTaskPath<>(value.attempt());
    }

    public VTaskPath<Unit> voided() {
        return new VTaskPath<>(value.voided());
    }

    public VTaskPath<A> timeout(Duration duration) {
        return new VTaskPath<>(value.timeout(duration));
    }

    public VTaskPath<A> onExecutor(Executor executor) {
        return new VTaskPath<>(value.onExecutor(executor));
    }

    public VTaskPath<A> race(VTaskPath<A> other) {
        return new VTaskPath<>(VTask.async(() -> {
            CompletableFuture<A> result = new CompletableFuture<>();
            value.runAsync().whenComplete((left, leftError) -> {
                if (!result.isDone()) {
                    if (leftError == null) result.complete(left);
                    else result.completeExceptionally(leftError);
                }
            });
            other.value.runAsync().whenComplete((right, rightError) -> {
                if (!result.isDone()) {
                    if (rightError == null) result.complete(right);
                    else result.completeExceptionally(rightError);
                }
            });
            return result;
        }));
    }

    public VTaskPath<Maybe<A>> asMaybe() {
        return new VTaskPath<>(value.attempt().map(Either::toMaybe));
    }

    public VTaskPath<Try<A>> asTry() {
        return new VTaskPath<>(value.attempt().map(either -> either.fold(Try::failure, Try::success)));
    }

    public TryPath<A> toTryPath() {
        return new TryPath<>(value.runSafe());
    }

    public IOPath<A> toIOPath() {
        return Pathway.io(value::unsafeRun);
    }

    public ResourcePath<A> asResource(Function<? super A, VTaskPath<Unit>> release) {
        Objects.requireNonNull(release, "release");
        return new ResourcePath<>(value.asResource(resource -> release.apply(resource).run()));
    }


    public VTaskPath<A> retry(RetryPolicy policy) {
        return new VTaskPath<>(value.retry(policy));
    }

    public VTaskPath<A> circuitBreaker(CircuitBreaker circuitBreaker) {
        return new VTaskPath<>(value.circuitBreaker(circuitBreaker));
    }

    public VTaskPath<A> bulkhead(Bulkhead bulkhead) {
        return new VTaskPath<>(value.bulkhead(bulkhead));
    }

    public ResilienceBuilder<A> resilient() {
        return Resilience.builder(value);
    }
}

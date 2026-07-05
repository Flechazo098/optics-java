package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.business.capability.Effectful;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.function.Function3;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TaskPath<A> implements Combinable<A>, Effectful<A> {
    private final Task<A> value;

    public TaskPath(Task<A> value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public Task<A> run() {
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
    public <B> TaskPath<B> map(Function<? super A, ? extends B> mapper) {
        return new TaskPath<>(value.map(mapper));
    }

    @Override
    public <B> TaskPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new TaskPath<>(value.flatMap(a -> {
            Chainable<B> mapped = mapper.apply(a);
            if (!(mapped instanceof TaskPath<?> taskPath)) {
                throw new IllegalArgumentException("via mapper must return TaskPath, got: " + mapped.getClass());
            }
            return ((TaskPath<B>) taskPath).run();
        }));
    }

    @Override
    public <B, C> TaskPath<C> zipWith(Combinable<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof TaskPath<?> otherTask)) {
            throw new IllegalArgumentException("Cannot zipWith non-TaskPath: " + other.getClass());
        }
        return new TaskPath<>(value.zipWith(((TaskPath<B>) otherTask).value, combiner));
    }

    public <B, C> TaskPath<C> parZipWith(TaskPath<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        return new TaskPath<>(value.parZipWith(other.value, combiner));
    }

    @Override
    public <B> TaskPath<B> then(Supplier<? extends Chainable<B>> next) {
        Objects.requireNonNull(next, "next");
        return via(ignored -> next.get());
    }

    public TaskPath<A> recover(Function<? super Throwable, ? extends A> recovery) {
        return new TaskPath<>(value.recover(recovery));
    }

    public TaskPath<A> recoverWith(Function<? super Throwable, TaskPath<A>> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        return new TaskPath<>(value.recoverWith(error -> recovery.apply(error).run()));
    }

    public TaskPath<A> handleError(Function<? super Throwable, ? extends A> recovery) {
        return recover(recovery);
    }

    public TaskPath<A> handleErrorWith(Function<? super Throwable, ? extends Effectful<A>> recovery) {
        return new TaskPath<>(value.recoverWith(error -> {
            Effectful<A> result = recovery.apply(error);
            if (result instanceof TaskPath<?> taskPath) {
                return ((TaskPath<A>) taskPath).run();
            }
            return Task.delay(result::unsafeRun);
        }));
    }

    public TaskPath<A> guarantee(Runnable finalizer) {
        return new TaskPath<>(Task.delay(() -> {
            try {
                return value.unsafeRun();
            } finally {
                finalizer.run();
            }
        }));
    }

    public TaskPath<A> mapError(Function<? super Throwable, ? extends Throwable> mapper) {
        return new TaskPath<>(value.mapError(mapper));
    }

    public TaskPath<A> peek(Consumer<? super A> action) {
        return new TaskPath<>(value.peek(action));
    }

    public TaskPath<A> peekFailure(Consumer<? super Throwable> action) {
        return new TaskPath<>(value.peekFailure(action));
    }

    public TaskPath<Either<Throwable, A>> attempt() {
        return new TaskPath<>(value.attempt());
    }

    public TaskPath<Unit> voided() {
        return new TaskPath<>(value.voided());
    }

    public TaskPath<A> timeout(Duration duration) {
        return new TaskPath<>(value.timeout(duration));
    }

    public TaskPath<A> onExecutor(Executor executor) {
        return new TaskPath<>(value.onExecutor(executor));
    }

    public TaskPath<A> race(TaskPath<A> other) {
        return new TaskPath<>(Task.async(() -> {
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

    public TaskPath<Maybe<A>> asMaybe() {
        return new TaskPath<>(value.attempt().map(Either::toMaybe));
    }

    public TaskPath<Try<A>> asTry() {
        return new TaskPath<>(value.attempt().map(either -> either.fold(Try::failure, Try::success)));
    }

    public TryPath<A> toTryPath() {
        return new TryPath<>(value.runSafe());
    }

    public IOPath<A> toIOPath() {
        return Pathway.io(value::unsafeRun);
    }

    public <B, C, D> TaskPath<D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        if (!(second instanceof TaskPath<?> secondTask)) {
            throw new IllegalArgumentException("second must be TaskPath, got: " + second.getClass());
        }
        if (!(third instanceof TaskPath<?> thirdTask)) {
            throw new IllegalArgumentException("third must be TaskPath, got: " + third.getClass());
        }
        return zipWith((TaskPath<B>) secondTask, Combinable.Pair2::new)
                .zipWith((TaskPath<C>) thirdTask, (pair, c) -> combiner.apply(pair.first(), pair.second(), c));
    }

    public TaskPath<A> retry(RetryPolicy policy, ScheduledExecutorService scheduler) {
        return new TaskPath<>(value.retry(policy, scheduler));
    }
}

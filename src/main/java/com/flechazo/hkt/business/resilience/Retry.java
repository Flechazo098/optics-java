package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.VTask;
import com.flechazo.hkt.business.effect.IO;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Retry {
    private Retry() {
    }

    public static <A> A execute(RetryPolicy policy, Supplier<A> supplier) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(supplier, "supplier");
        Throwable lastError = null;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            try {
                return supplier.get();
            } catch (Throwable error) {
                lastError = error;
                if (!policy.shouldRetry(error) || attempt >= policy.maxAttempts()) {
                    break;
                }
                Duration delay = policy.delayForAttempt(attempt);
                policy.retryListener().accept(RetryEvent.of(attempt, error, delay));
                sleep(delay, attempt, error);
            }
        }
        throw RetryExhaustedException.of(lastError, policy.maxAttempts());
    }

    public static void execute(RetryPolicy policy, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        execute(policy, () -> {
            runnable.run();
            return Unit.INSTANCE;
        });
    }

    public static <A> VTask<A> retryVTask(VTask<A> task, RetryPolicy policy) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(policy, "policy");
        return () -> executeVTask(policy, task::execute);
    }

    public static <A> IO<A> retryIO(IO<A> io, RetryPolicy policy) {
        Objects.requireNonNull(io, "io");
        Objects.requireNonNull(policy, "policy");
        return () -> {
            try {
                return executeVTask(policy, io::unsafeRun);
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

    public static <A> VTask<A> retryVTaskWithFallback(VTask<A> task, RetryPolicy policy, Function<Throwable, ? extends A> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return retryVTask(task, policy).recover(error -> {
            Throwable cause = error instanceof RetryExhaustedException exhausted && exhausted.getCause() != null
                    ? exhausted.getCause()
                    : error;
            return fallback.apply(cause);
        });
    }

    public static <A> VTask<A> retryVTaskWithRecovery(VTask<A> task, RetryPolicy policy, Function<Throwable, ? extends VTask<A>> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        return retryVTask(task, policy).recoverWith(error -> {
            Throwable cause = error instanceof RetryExhaustedException exhausted && exhausted.getCause() != null
                    ? exhausted.getCause()
                    : error;
            return recovery.apply(cause);
        });
    }

    private static <A> A executeVTask(RetryPolicy policy, ThrowingSupplier<A> supplier) throws Throwable {
        Throwable lastError = null;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            try {
                return supplier.get();
            } catch (Throwable error) {
                lastError = error;
                if (!policy.shouldRetry(error) || attempt >= policy.maxAttempts()) {
                    break;
                }
                Duration delay = policy.delayForAttempt(attempt);
                policy.retryListener().accept(RetryEvent.of(attempt, error, delay));
                sleep(delay, attempt, error);
            }
        }
        throw RetryExhaustedException.of(lastError, policy.maxAttempts());
    }

    private static void sleep(Duration delay, int attempt, Throwable lastError) {
        if (delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RetryExhaustedException("Retry interrupted after " + attempt + " attempts", lastError, attempt);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<A> {
        A get() throws Throwable;
    }
}

package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.effect.VIO;

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

    public static <A> Task<A> retryTask(Task<A> task, RetryPolicy policy) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(policy, "policy");
        return () -> executeTask(policy, task::execute);
    }

    public static <A> VIO<A> retryVIO(VIO<A> vio, RetryPolicy policy) {
        Objects.requireNonNull(vio, "vio");
        Objects.requireNonNull(policy, "policy");
        return () -> {
            try {
                return executeTask(policy, vio::unsafeRun);
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

    public static <A> Task<A> retryTaskWithFallback(Task<A> task, RetryPolicy policy, Function<Throwable, ? extends A> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return retryTask(task, policy).recover(error -> {
            Throwable cause = error instanceof RetryExhaustedException exhausted && exhausted.getCause() != null
                    ? exhausted.getCause()
                    : error;
            return fallback.apply(cause);
        });
    }

    public static <A> Task<A> retryTaskWithRecovery(Task<A> task, RetryPolicy policy, Function<Throwable, ? extends Task<A>> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        return retryTask(task, policy).recoverWith(error -> {
            Throwable cause = error instanceof RetryExhaustedException exhausted && exhausted.getCause() != null
                    ? exhausted.getCause()
                    : error;
            return recovery.apply(cause);
        });
    }

    private static <A> A executeTask(RetryPolicy policy, ThrowingSupplier<A> supplier) throws Throwable {
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

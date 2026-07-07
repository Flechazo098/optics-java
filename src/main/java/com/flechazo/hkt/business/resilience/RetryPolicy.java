package com.flechazo.hkt.business.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class RetryPolicy {
    private static final Consumer<RetryEvent> NOOP_LISTENER = ignored -> {
    };

    enum BackoffStrategy {
        EXPONENTIAL,
        LINEAR
    }

    private final int maxAttempts;
    private final Duration initialDelay;
    private final double backoffMultiplier;
    private final Duration maxDelay;
    private final boolean jitter;
    private final Predicate<Throwable> retryPredicate;
    private final Consumer<RetryEvent> retryListener;
    private final BackoffStrategy strategy;

    private RetryPolicy(
            int maxAttempts,
            Duration initialDelay,
            double backoffMultiplier,
            Duration maxDelay,
            boolean jitter,
            Predicate<Throwable> retryPredicate,
            Consumer<RetryEvent> retryListener,
            BackoffStrategy strategy) {
        this.maxAttempts = maxAttempts;
        this.initialDelay = Objects.requireNonNull(initialDelay, "initialDelay");
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelay = Objects.requireNonNull(maxDelay, "maxDelay");
        this.jitter = jitter;
        this.retryPredicate = Objects.requireNonNull(retryPredicate, "retryPredicate");
        this.retryListener = Objects.requireNonNull(retryListener, "retryListener");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public static RetryPolicy fixed(int maxAttempts, Duration delay) {
        validateMaxAttempts(maxAttempts);
        Objects.requireNonNull(delay, "delay");
        return new RetryPolicy(maxAttempts, delay, 1.0, delay, false, ignored -> true, NOOP_LISTENER, BackoffStrategy.EXPONENTIAL);
    }

    public static RetryPolicy fixedDelay(int maxRetries, Duration delay) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        return fixed(maxRetries + 1, delay);
    }

    public static RetryPolicy exponentialBackoff(int maxAttempts, Duration initialDelay) {
        validateMaxAttempts(maxAttempts);
        return new RetryPolicy(maxAttempts, initialDelay, 2.0, Duration.ofMinutes(5), false, ignored -> true, NOOP_LISTENER, BackoffStrategy.EXPONENTIAL);
    }

    public static RetryPolicy exponentialBackoff(int maxRetries, Duration initialDelay, Duration maxDelay) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        return exponentialBackoff(maxRetries + 1, initialDelay).withMaxDelay(maxDelay);
    }

    public static RetryPolicy exponentialBackoffWithJitter(int maxAttempts, Duration initialDelay) {
        validateMaxAttempts(maxAttempts);
        return new RetryPolicy(maxAttempts, initialDelay, 2.0, Duration.ofMinutes(5), true, ignored -> true, NOOP_LISTENER, BackoffStrategy.EXPONENTIAL);
    }

    public static RetryPolicy linear(int maxAttempts, Duration initialDelay) {
        validateMaxAttempts(maxAttempts);
        return new RetryPolicy(maxAttempts, initialDelay, 1.0, Duration.ofMinutes(5), false, ignored -> true, NOOP_LISTENER, BackoffStrategy.LINEAR);
    }

    public static RetryPolicy noRetry() {
        return new RetryPolicy(1, Duration.ZERO, 1.0, Duration.ZERO, false, ignored -> false, NOOP_LISTENER, BackoffStrategy.EXPONENTIAL);
    }

    public static RetryPolicy never() {
        return noRetry();
    }

    public static Builder builder() {
        return new Builder();
    }

    public RetryPolicy withMaxAttempts(int maxAttempts) {
        validateMaxAttempts(maxAttempts);
        return copy(maxAttempts, initialDelay, backoffMultiplier, maxDelay, jitter, retryPredicate, retryListener, strategy);
    }

    public RetryPolicy withInitialDelay(Duration initialDelay) {
        return copy(maxAttempts, initialDelay, backoffMultiplier, maxDelay, jitter, retryPredicate, retryListener, strategy);
    }

    public RetryPolicy withBackoffMultiplier(double multiplier) {
        if (multiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be at least 1.0");
        }
        return copy(maxAttempts, initialDelay, multiplier, maxDelay, jitter, retryPredicate, retryListener, strategy);
    }

    public RetryPolicy withMaxDelay(Duration maxDelay) {
        return copy(maxAttempts, initialDelay, backoffMultiplier, maxDelay, jitter, retryPredicate, retryListener, strategy);
    }

    public RetryPolicy retryOn(Class<? extends Throwable> errorType) {
        Objects.requireNonNull(errorType, "errorType");
        return retryIf(errorType::isInstance);
    }

    public RetryPolicy retryIf(Predicate<Throwable> predicate) {
        return copy(maxAttempts, initialDelay, backoffMultiplier, maxDelay, jitter, predicate, retryListener, strategy);
    }

    public RetryPolicy onRetry(Consumer<RetryEvent> listener) {
        return copy(maxAttempts, initialDelay, backoffMultiplier, maxDelay, jitter, retryPredicate, Objects.requireNonNull(listener, "listener"), strategy);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public Duration initialDelay() {
        return initialDelay;
    }

    public double backoffMultiplier() {
        return backoffMultiplier;
    }

    public Duration maxDelay() {
        return maxDelay;
    }

    public boolean useJitter() {
        return jitter;
    }

    public boolean shouldRetry(Throwable error) {
        return retryPredicate.test(error);
    }

    public Consumer<RetryEvent> retryListener() {
        return retryListener;
    }

    public Duration delayForAttempt(int attemptNumber) {
        if (attemptNumber <= 1) {
            return initialDelay;
        }

        long delayMillis;
        if (strategy == BackoffStrategy.LINEAR) {
            delayMillis = multiplyToMillis(initialDelay, attemptNumber);
        } else {
            delayMillis = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptNumber - 1));
        }
        delayMillis = Math.min(delayMillis, maxDelay.toMillis());
        if (jitter && delayMillis > 0) {
            delayMillis = ThreadLocalRandom.current().nextLong(delayMillis + 1);
        }
        return Duration.ofMillis(delayMillis);
    }

    private RetryPolicy copy(
            int nextMaxAttempts,
            Duration nextInitialDelay,
            double nextMultiplier,
            Duration nextMaxDelay,
            boolean nextJitter,
            Predicate<Throwable> nextPredicate,
            Consumer<RetryEvent> nextListener,
            BackoffStrategy nextStrategy) {
        Objects.requireNonNull(nextInitialDelay, "initialDelay");
        Objects.requireNonNull(nextMaxDelay, "maxDelay");
        if (nextInitialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay must not be negative");
        }
        if (nextMaxDelay.isNegative()) {
            throw new IllegalArgumentException("maxDelay must not be negative");
        }
        return new RetryPolicy(nextMaxAttempts, nextInitialDelay, nextMultiplier, nextMaxDelay, nextJitter, nextPredicate, nextListener, nextStrategy);
    }

    private static long multiplyToMillis(Duration duration, int multiplier) {
        return Math.multiplyExact(duration.toMillis(), multiplier);
    }

    private static void validateMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
    }

    public static final class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private double backoffMultiplier = 2.0;
        private Duration maxDelay = Duration.ofMinutes(5);
        private boolean jitter;
        private Predicate<Throwable> retryPredicate = ignored -> true;
        private Consumer<RetryEvent> retryListener = NOOP_LISTENER;
        private BackoffStrategy strategy = BackoffStrategy.EXPONENTIAL;

        private Builder() {
        }

        public Builder maxAttempts(int value) {
            validateMaxAttempts(value);
            maxAttempts = value;
            return this;
        }

        public Builder initialDelay(Duration value) {
            initialDelay = Objects.requireNonNull(value, "initialDelay");
            return this;
        }

        public Builder backoffMultiplier(double value) {
            if (value < 1.0) {
                throw new IllegalArgumentException("backoffMultiplier must be at least 1.0");
            }
            backoffMultiplier = value;
            return this;
        }

        public Builder maxDelay(Duration value) {
            maxDelay = Objects.requireNonNull(value, "maxDelay");
            return this;
        }

        public Builder useJitter(boolean value) {
            jitter = value;
            return this;
        }

        public Builder retryIf(Predicate<Throwable> value) {
            retryPredicate = Objects.requireNonNull(value, "retryPredicate");
            return this;
        }

        public Builder retryOn(Class<? extends Throwable> value) {
            Objects.requireNonNull(value, "errorType");
            retryPredicate = value::isInstance;
            return this;
        }

        public Builder onRetry(Consumer<RetryEvent> value) {
            retryListener = Objects.requireNonNull(value, "retryListener");
            return this;
        }

        public Builder linearBackoff() {
            strategy = BackoffStrategy.LINEAR;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(maxAttempts, initialDelay, backoffMultiplier, maxDelay, jitter, retryPredicate, retryListener, strategy);
        }
    }
}

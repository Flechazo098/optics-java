package com.flechazo.hkt.business.core;

import com.flechazo.hkt.Maybe;

import java.time.Duration;
import java.util.Objects;

/**
 * Determines whether a failed operation should be retried and how long to delay.
 */
@FunctionalInterface
public interface RetryPolicy {
    /**
     * Returns the delay before the next retry.
     *
     * @param attempt the zero-based retry attempt
     * @param error the failure that triggered the decision
     * @return the next delay, or an empty value when no further retry is allowed
     */
    Maybe<Duration> nextDelay(int attempt, Throwable error);

    /**
     * Creates a policy that never retries.
     *
     * @return a policy that always returns an empty delay
     */
    static RetryPolicy never() {
        return (attempt, error) -> Maybe.none();
    }

    /**
     * Creates a policy with a constant delay and bounded retry count.
     *
     * @param maxRetries the maximum number of retries
     * @param delay the delay before each retry
     * @return the fixed-delay policy
     * @throws IllegalArgumentException if {@code maxRetries} is negative
     */
    static RetryPolicy fixedDelay(int maxRetries, Duration delay) {
        Objects.requireNonNull(delay, "delay");
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        return (attempt, error) -> attempt < maxRetries ? Maybe.some(delay) : Maybe.none();
    }

    /**
     * Creates a bounded exponential-backoff policy.
     *
     * @param maxRetries the maximum number of retries
     * @param initial the delay before the first retry
     * @param max the maximum permitted delay
     * @return the exponential-backoff policy
     * @throws IllegalArgumentException if the retry count is negative, the initial delay is not positive,
     *         or the maximum delay is less than the initial delay
     */
    static RetryPolicy exponentialBackoff(int maxRetries, Duration initial, Duration max) {
        Objects.requireNonNull(initial, "initial");
        Objects.requireNonNull(max, "max");
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        if (initial.isNegative() || initial.isZero()) {
            throw new IllegalArgumentException("initial must be positive");
        }
        if (max.compareTo(initial) < 0) {
            throw new IllegalArgumentException("max must be greater than or equal to initial");
        }
        return (attempt, error) -> {
            if (attempt >= maxRetries) {
                return Maybe.none();
            }
            long multiplier = 1L << Math.min(attempt, 30);
            Duration delay = initial.multipliedBy(multiplier);
            return Maybe.some(delay.compareTo(max) > 0 ? max : delay);
        };
    }
}

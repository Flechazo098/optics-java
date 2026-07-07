package com.flechazo.hkt.business.core;

import com.flechazo.hkt.Maybe;

import java.time.Duration;
import java.util.Objects;

@FunctionalInterface
public interface RetryPolicy {
    Maybe<Duration> nextDelay(int attempt, Throwable error);

    static RetryPolicy never() {
        return (attempt, error) -> Maybe.none();
    }

    static RetryPolicy fixedDelay(int maxRetries, Duration delay) {
        Objects.requireNonNull(delay, "delay");
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        return (attempt, error) -> attempt < maxRetries ? Maybe.some(delay) : Maybe.none();
    }

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

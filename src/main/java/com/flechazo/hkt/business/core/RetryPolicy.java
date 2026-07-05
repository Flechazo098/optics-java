package com.flechazo.hkt.business.core;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface RetryPolicy {
    Optional<Duration> nextDelay(int attempt, Throwable error);

    static RetryPolicy never() {
        return (attempt, error) -> Optional.empty();
    }

    static RetryPolicy fixedDelay(int maxRetries, Duration delay) {
        Objects.requireNonNull(delay, "delay");
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        return (attempt, error) -> attempt < maxRetries ? Optional.of(delay) : Optional.empty();
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
                return Optional.empty();
            }
            long multiplier = 1L << Math.min(attempt, 30);
            Duration delay = initial.multipliedBy(multiplier);
            return Optional.of(delay.compareTo(max) > 0 ? max : delay);
        };
    }
}

package com.flechazo.hkt.business.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record RetryEvent(int attemptNumber, Throwable lastError, Duration nextDelay, Instant timestamp) {
    public RetryEvent {
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber must be positive");
        }
        Objects.requireNonNull(lastError, "lastError");
        Objects.requireNonNull(nextDelay, "nextDelay");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public static RetryEvent of(int attemptNumber, Throwable lastError, Duration nextDelay) {
        return new RetryEvent(attemptNumber, lastError, nextDelay, Instant.now());
    }
}

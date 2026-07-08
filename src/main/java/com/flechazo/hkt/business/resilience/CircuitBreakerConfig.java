package com.flechazo.hkt.business.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

public record CircuitBreakerConfig(
        int failureThreshold,
        int successThreshold,
        Duration openDuration,
        Duration callTimeout,
        Predicate<Throwable> recordFailure) {
    public static final int DEFAULT_FAILURE_THRESHOLD = 5;
    public static final int DEFAULT_SUCCESS_THRESHOLD = 1;
    public static final Duration DEFAULT_OPEN_DURATION = Duration.ofSeconds(60);
    public static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(10);

    public CircuitBreakerConfig {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be at least 1");
        }
        if (successThreshold < 1) {
            throw new IllegalArgumentException("successThreshold must be at least 1");
        }
        Objects.requireNonNull(openDuration, "openDuration");
        Objects.requireNonNull(callTimeout, "callTimeout");
        Objects.requireNonNull(recordFailure, "recordFailure");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CircuitBreakerConfig defaults() {
        return builder().build();
    }

    public static final class Builder {
        private int failureThreshold = DEFAULT_FAILURE_THRESHOLD;
        private int successThreshold = DEFAULT_SUCCESS_THRESHOLD;
        private Duration openDuration = DEFAULT_OPEN_DURATION;
        private Duration callTimeout = DEFAULT_CALL_TIMEOUT;
        private Predicate<Throwable> recordFailure = ignored -> true;

        private Builder() {
        }

        public Builder failureThreshold(int value) {
            failureThreshold = value;
            return this;
        }

        public Builder successThreshold(int value) {
            successThreshold = value;
            return this;
        }

        public Builder openDuration(Duration value) {
            openDuration = Objects.requireNonNull(value, "openDuration");
            return this;
        }

        public Builder callTimeout(Duration value) {
            callTimeout = Objects.requireNonNull(value, "callTimeout");
            return this;
        }

        public Builder recordFailure(Predicate<Throwable> value) {
            recordFailure = Objects.requireNonNull(value, "recordFailure");
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(failureThreshold, successThreshold, openDuration, callTimeout, recordFailure);
        }
    }
}

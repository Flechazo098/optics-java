package com.flechazo.hkt.business.resilience;

import java.time.Duration;
import java.util.Objects;

public class CircuitOpenException extends RuntimeException {
    private final CircuitBreaker.Status status;
    private final Duration retryAfter;

    public CircuitOpenException(CircuitBreaker.Status status, Duration retryAfter) {
        super("Circuit breaker is " + status + ", retry after " + retryAfter);
        this.status = Objects.requireNonNull(status, "status");
        this.retryAfter = Objects.requireNonNull(retryAfter, "retryAfter");
    }

    public CircuitBreaker.Status status() {
        return status;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}

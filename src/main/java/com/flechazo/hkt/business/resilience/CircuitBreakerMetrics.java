package com.flechazo.hkt.business.resilience;

import java.time.Instant;
import java.util.Objects;

public record CircuitBreakerMetrics(
        long totalCalls,
        long successfulCalls,
        long failedCalls,
        long rejectedCalls,
        long stateTransitions,
        Instant lastStateChange) {
    public CircuitBreakerMetrics {
        Objects.requireNonNull(lastStateChange, "lastStateChange");
    }
}

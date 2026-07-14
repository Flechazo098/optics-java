package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.business.effect.VTask;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

public final class ResilienceBuilder<A> {
    private final VTask<A> task;
    private CircuitBreaker circuitBreaker;
    private RetryPolicy retryPolicy;
    private Bulkhead bulkhead;
    private Duration timeout;
    private Function<Throwable, ? extends A> fallback;

    private ResilienceBuilder(VTask<A> task) {
        this.task = Objects.requireNonNull(task, "task");
    }

    static <A> ResilienceBuilder<A> of(VTask<A> task) {
        return new ResilienceBuilder<>(task);
    }

    public ResilienceBuilder<A> withCircuitBreaker(CircuitBreaker value) {
        circuitBreaker = Objects.requireNonNull(value, "circuitBreaker");
        return this;
    }

    public ResilienceBuilder<A> withCircuitBreaker(CircuitBreakerConfig config) {
        circuitBreaker = CircuitBreaker.create(config);
        return this;
    }

    public ResilienceBuilder<A> withRetry(RetryPolicy value) {
        retryPolicy = Objects.requireNonNull(value, "retryPolicy");
        return this;
    }

    public ResilienceBuilder<A> withBulkhead(Bulkhead value) {
        bulkhead = Objects.requireNonNull(value, "bulkhead");
        return this;
    }

    public ResilienceBuilder<A> withBulkhead(BulkheadConfig config) {
        bulkhead = Bulkhead.create(config);
        return this;
    }

    public ResilienceBuilder<A> withTimeout(Duration value) {
        timeout = Objects.requireNonNull(value, "timeout");
        return this;
    }

    public ResilienceBuilder<A> withFallback(Function<Throwable, ? extends A> value) {
        fallback = Objects.requireNonNull(value, "fallback");
        return this;
    }

    public VTask<A> build() {
        VTask<A> result = task;
        if (circuitBreaker != null) {
            result = circuitBreaker.protect(result);
        }
        if (retryPolicy != null) {
            result = Retry.retryVTask(result, retryPolicy);
        }
        if (bulkhead != null) {
            result = bulkhead.protect(result);
        }
        if (timeout != null) {
            result = result.timeout(timeout);
        }
        if (fallback != null) {
            result = result.recover(fallback);
        }
        return result;
    }
}

package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.business.effect.VTask;
import com.flechazo.hkt.business.stream.VStream;

import java.util.Objects;
import java.util.function.Function;

public final class Resilience {
    private Resilience() {
    }

    public static <A> ResilienceBuilder<A> builder(VTask<A> task) {
        return ResilienceBuilder.of(task);
    }

    public static <A> VTask<A> withCircuitBreakerAndRetry(VTask<A> task, CircuitBreaker circuitBreaker, RetryPolicy retryPolicy) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        return Retry.retryVTask(circuitBreaker.protect(task), retryPolicy);
    }

    public static <A> VTask<A> protect(VTask<A> task, CircuitBreaker circuitBreaker, RetryPolicy retryPolicy, Bulkhead bulkhead) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        Objects.requireNonNull(bulkhead, "bulkhead");
        return bulkhead.protect(Retry.retryVTask(circuitBreaker.protect(task), retryPolicy));
    }

    public static <A, B> Function<A, VTask<B>> withRetryPerElement(Function<? super A, ? extends VTask<B>> mapper, RetryPolicy policy) {
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(policy, "policy");
        return value -> Retry.retryVTask(mapper.apply(value), policy);
    }

    public static <A, B> Function<A, VTask<B>> withCircuitBreakerPerElement(Function<? super A, ? extends VTask<B>> mapper, CircuitBreaker circuitBreaker) {
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        return value -> circuitBreaker.protect(mapper.apply(value));
    }

    public static <A, B> VStream<B> mapVTaskWithRetry(VStream<A> stream, RetryPolicy policy, Function<? super A, ? extends VTask<B>> mapper) {
        Objects.requireNonNull(stream, "stream");
        return stream.mapVTask(withRetryPerElement(mapper, policy));
    }
}

package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.stream.VStream;

import java.util.Objects;
import java.util.function.Function;

public final class Resilience {
    private Resilience() {
    }

    public static <A> ResilienceBuilder<A> builder(Task<A> task) {
        return ResilienceBuilder.of(task);
    }

    public static <A> Task<A> withCircuitBreakerAndRetry(Task<A> task, CircuitBreaker circuitBreaker, RetryPolicy retryPolicy) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        return Retry.retryTask(circuitBreaker.protect(task), retryPolicy);
    }

    public static <A> Task<A> protect(Task<A> task, CircuitBreaker circuitBreaker, RetryPolicy retryPolicy, Bulkhead bulkhead) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        Objects.requireNonNull(bulkhead, "bulkhead");
        return bulkhead.protect(Retry.retryTask(circuitBreaker.protect(task), retryPolicy));
    }

    public static <A, B> Function<A, Task<B>> withRetryPerElement(Function<? super A, ? extends Task<B>> mapper, RetryPolicy policy) {
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(policy, "policy");
        return value -> Retry.retryTask(mapper.apply(value), policy);
    }

    public static <A, B> Function<A, Task<B>> withCircuitBreakerPerElement(Function<? super A, ? extends Task<B>> mapper, CircuitBreaker circuitBreaker) {
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        return value -> circuitBreaker.protect(mapper.apply(value));
    }

    public static <A, B> VStream<B> mapTaskWithRetry(VStream<A> stream, RetryPolicy policy, Function<? super A, ? extends Task<B>> mapper) {
        Objects.requireNonNull(stream, "stream");
        return stream.mapTask(withRetryPerElement(mapper, policy));
    }
}

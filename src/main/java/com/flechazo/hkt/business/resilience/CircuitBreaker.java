package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.business.effect.IO;
import com.flechazo.hkt.business.effect.VTask;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class CircuitBreaker {
    public enum Status {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private record InternalState(
            Status status, int failureCount, int successCount, int inFlightProbes, Instant lastStateChange) {
    }

    private enum ProbeDecision {
        ACQUIRED,
        NOT_REQUIRED,
        REJECTED
    }

    private final CircuitBreakerConfig config;
    private final AtomicReference<InternalState> state;
    private final AtomicLong totalCalls = new AtomicLong();
    private final AtomicLong successfulCalls = new AtomicLong();
    private final AtomicLong failedCalls = new AtomicLong();
    private final AtomicLong rejectedCalls = new AtomicLong();
    private final AtomicLong stateTransitions = new AtomicLong();

    private CircuitBreaker(CircuitBreakerConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.state = new AtomicReference<>(new InternalState(Status.CLOSED, 0, 0, 0, Instant.now()));
    }

    public static CircuitBreaker create(CircuitBreakerConfig config) {
        return new CircuitBreaker(config);
    }

    public static CircuitBreaker withDefaults() {
        return new CircuitBreaker(CircuitBreakerConfig.defaults());
    }

    public <A> VTask<A> protect(VTask<A> task) {
        Objects.requireNonNull(task, "task");
        return () -> {
            totalCalls.incrementAndGet();
            InternalState current = refreshOpenState();
            if (current.status() == Status.OPEN) {
                rejectedCalls.incrementAndGet();
                throw new CircuitOpenException(Status.OPEN, remainingOpenDuration(current));
            }
            boolean probeAcquired = false;
            if (current.status() == Status.HALF_OPEN) {
                switch (tryAcquireProbe()) {
                    case ACQUIRED -> probeAcquired = true;
                    case NOT_REQUIRED -> {
                    }
                    case REJECTED -> {
                        rejectedCalls.incrementAndGet();
                        InternalState now = state.get();
                        throw new CircuitOpenException(
                                now.status(),
                                now.status() == Status.OPEN ? remainingOpenDuration(now) : Duration.ZERO);
                    }
                }
            }
            try {
                A result = task.timeout(config.callTimeout()).execute();
                onSuccess(probeAcquired);
                return result;
            } catch (Throwable error) {
                if (config.recordFailure().test(error)) {
                    onFailure();
                } else {
                    onSuccess(probeAcquired);
                }
                throw error;
            }
        };
    }

    public <A> IO<A> protect(IO<A> io) {
        Objects.requireNonNull(io, "io");
        return () -> {
            try {
                return protect(io.toVTask()).execute();
            } catch (Exception exception) {
                throw exception;
            } catch (Throwable throwable) {
                if (throwable instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException(throwable);
            }
        };
    }

    public <A> VTask<A> protectWithFallback(VTask<A> task, Function<Throwable, ? extends A> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return protect(task).recover(error -> {
            if (error instanceof CircuitOpenException) {
                return fallback.apply(error);
            }
            throw rethrow(error);
        });
    }

    public Status currentStatus() {
        return refreshOpenState().status();
    }

    public CircuitBreakerMetrics metrics() {
        return new CircuitBreakerMetrics(
                totalCalls.get(),
                successfulCalls.get(),
                failedCalls.get(),
                rejectedCalls.get(),
                stateTransitions.get(),
                state.get().lastStateChange());
    }

    public void reset() {
        state.set(new InternalState(Status.CLOSED, 0, 0, 0, Instant.now()));
        stateTransitions.incrementAndGet();
    }

    public void tripOpen() {
        state.set(new InternalState(Status.OPEN, 0, 0, 0, Instant.now()));
        stateTransitions.incrementAndGet();
    }

    private InternalState refreshOpenState() {
        while (true) {
            InternalState current = state.get();
            if (current.status() != Status.OPEN || remainingOpenDuration(current).compareTo(Duration.ZERO) > 0) {
                return current;
            }
            InternalState next = new InternalState(Status.HALF_OPEN, 0, 0, 0, Instant.now());
            if (state.compareAndSet(current, next)) {
                stateTransitions.incrementAndGet();
                return next;
            }
        }
    }

    // Concurrent half-open probes are capped at successThreshold: exactly as many
    // trial calls as are needed to close the circuit may be in flight at once.
    private ProbeDecision tryAcquireProbe() {
        while (true) {
            InternalState current = state.get();
            if (current.status() == Status.CLOSED) {
                return ProbeDecision.NOT_REQUIRED;
            }
            if (current.status() == Status.OPEN || current.inFlightProbes() >= config.successThreshold()) {
                return ProbeDecision.REJECTED;
            }
            InternalState next = new InternalState(
                    Status.HALF_OPEN,
                    current.failureCount(),
                    current.successCount(),
                    current.inFlightProbes() + 1,
                    current.lastStateChange());
            if (state.compareAndSet(current, next)) {
                return ProbeDecision.ACQUIRED;
            }
        }
    }

    private Duration remainingOpenDuration(InternalState current) {
        Duration elapsed = Duration.between(current.lastStateChange(), Instant.now());
        Duration remaining = config.openDuration().minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private void onSuccess(boolean probeAcquired) {
        successfulCalls.incrementAndGet();
        InternalState previous = state.getAndUpdate(current -> switch (current.status()) {
            case CLOSED -> current.failureCount() == 0
                    ? current
                    : new InternalState(Status.CLOSED, 0, 0, 0, current.lastStateChange());
            case OPEN -> current;
            case HALF_OPEN -> {
                int nextSuccess = current.successCount() + 1;
                int nextInFlight = probeAcquired
                        ? Math.max(0, current.inFlightProbes() - 1)
                        : current.inFlightProbes();
                yield nextSuccess >= config.successThreshold()
                        ? new InternalState(Status.CLOSED, 0, 0, 0, Instant.now())
                        : new InternalState(Status.HALF_OPEN, 0, nextSuccess, nextInFlight, current.lastStateChange());
            }
        });
        if (previous.status() == Status.HALF_OPEN && previous.successCount() + 1 >= config.successThreshold()) {
            stateTransitions.incrementAndGet();
        }
    }

    private void onFailure() {
        failedCalls.incrementAndGet();
        InternalState previous = state.getAndUpdate(current -> switch (current.status()) {
            case CLOSED -> {
                int nextFailures = current.failureCount() + 1;
                yield nextFailures >= config.failureThreshold()
                        ? new InternalState(Status.OPEN, 0, 0, 0, Instant.now())
                        : new InternalState(Status.CLOSED, nextFailures, 0, 0, current.lastStateChange());
            }
            case HALF_OPEN -> new InternalState(Status.OPEN, 0, 0, 0, Instant.now());
            case OPEN -> current;
        });
        if ((previous.status() == Status.CLOSED && previous.failureCount() + 1 >= config.failureThreshold())
                || previous.status() == Status.HALF_OPEN) {
            stateTransitions.incrementAndGet();
        }
    }

    private static RuntimeException rethrow(Throwable error) {
        if (error instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (error instanceof Error fatal) {
            throw fatal;
        }
        return new RuntimeException(error);
    }
}

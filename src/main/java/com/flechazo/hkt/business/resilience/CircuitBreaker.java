package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.effect.VIO;

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

    private record InternalState(Status status, int failureCount, int successCount, Instant lastStateChange) {
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
        this.state = new AtomicReference<>(new InternalState(Status.CLOSED, 0, 0, Instant.now()));
    }

    public static CircuitBreaker create(CircuitBreakerConfig config) {
        return new CircuitBreaker(config);
    }

    public static CircuitBreaker withDefaults() {
        return new CircuitBreaker(CircuitBreakerConfig.defaults());
    }

    public <A> Task<A> protect(Task<A> task) {
        Objects.requireNonNull(task, "task");
        return () -> {
            totalCalls.incrementAndGet();
            InternalState current = refreshOpenState();
            if (current.status() == Status.OPEN) {
                rejectedCalls.incrementAndGet();
                throw new CircuitOpenException(Status.OPEN, remainingOpenDuration(current));
            }
            try {
                A result = task.timeout(config.callTimeout()).execute();
                onSuccess();
                return result;
            } catch (Throwable error) {
                if (config.recordFailure().test(error)) {
                    onFailure();
                } else {
                    onSuccess();
                }
                throw error;
            }
        };
    }

    public <A> VIO<A> protect(VIO<A> vio) {
        Objects.requireNonNull(vio, "vio");
        return () -> {
            try {
                return protect(vio.toTask()).execute();
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

    public <A> Task<A> protectWithFallback(Task<A> task, Function<Throwable, ? extends A> fallback) {
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
        state.set(new InternalState(Status.CLOSED, 0, 0, Instant.now()));
        stateTransitions.incrementAndGet();
    }

    public void tripOpen() {
        state.set(new InternalState(Status.OPEN, 0, 0, Instant.now()));
        stateTransitions.incrementAndGet();
    }

    private InternalState refreshOpenState() {
        while (true) {
            InternalState current = state.get();
            if (current.status() != Status.OPEN || remainingOpenDuration(current).compareTo(Duration.ZERO) > 0) {
                return current;
            }
            InternalState next = new InternalState(Status.HALF_OPEN, 0, 0, Instant.now());
            if (state.compareAndSet(current, next)) {
                stateTransitions.incrementAndGet();
                return next;
            }
        }
    }

    private Duration remainingOpenDuration(InternalState current) {
        Duration elapsed = Duration.between(current.lastStateChange(), Instant.now());
        Duration remaining = config.openDuration().minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private void onSuccess() {
        successfulCalls.incrementAndGet();
        InternalState previous = state.getAndUpdate(current -> switch (current.status()) {
            case CLOSED -> current.failureCount() == 0
                    ? current
                    : new InternalState(Status.CLOSED, 0, 0, current.lastStateChange());
            case OPEN -> current;
            case HALF_OPEN -> {
                int nextSuccess = current.successCount() + 1;
                yield nextSuccess >= config.successThreshold()
                        ? new InternalState(Status.CLOSED, 0, 0, Instant.now())
                        : new InternalState(Status.HALF_OPEN, 0, nextSuccess, current.lastStateChange());
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
                        ? new InternalState(Status.OPEN, 0, 0, Instant.now())
                        : new InternalState(Status.CLOSED, nextFailures, 0, current.lastStateChange());
            }
            case HALF_OPEN -> new InternalState(Status.OPEN, 0, 0, Instant.now());
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

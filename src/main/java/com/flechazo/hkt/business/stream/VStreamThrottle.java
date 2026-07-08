package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.Task;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class VStreamThrottle {
    private VStreamThrottle() {
    }

    public static <A> VStream<A> throttle(VStream<A> stream, int maxElements, Duration window) {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(window, "window");
        if (maxElements <= 0) {
            throw new IllegalArgumentException("maxElements must be positive");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }

        AtomicReference<WindowState> state = new AtomicReference<>(new WindowState(System.nanoTime(), 0));
        return throttle(stream, maxElements, window.toNanos(), state);
    }

    public static <A> VStream<A> metered(VStream<A> stream, Duration interval) {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(interval, "interval");
        if (interval.isNegative()) {
            throw new IllegalArgumentException("interval must not be negative");
        }

        return new VStream<>() {
            @Override
            public Task<Step<A>> pull() {
                return stream.pull().map(step -> switch (step) {
                    case VStream.Emit<A> emit -> {
                        sleep(interval);
                        yield new VStream.Emit<>(emit.value(), VStreamThrottle.metered(emit.tail(), interval));
                    }
                    case VStream.Skip<A> skip -> new VStream.Skip<>(VStreamThrottle.metered(skip.tail(), interval));
                    case VStream.Done<A> ignored -> new VStream.Done<>();
                });
            }

            @Override
            public Task<Unit> close() {
                return stream.close();
            }
        };
    }

    private static <A> VStream<A> throttle(
            VStream<A> stream,
            int maxElements,
            long windowNanos,
            AtomicReference<WindowState> state) {
        return new VStream<>() {
            @Override
            public Task<Step<A>> pull() {
                return stream.pull().map(step -> switch (step) {
                    case VStream.Emit<A> emit -> {
                        awaitPermit(maxElements, windowNanos, state);
                        yield new VStream.Emit<>(
                                emit.value(),
                                VStreamThrottle.throttle(emit.tail(), maxElements, windowNanos, state));
                    }
                    case VStream.Skip<A> skip ->
                            new VStream.Skip<>(VStreamThrottle.throttle(skip.tail(), maxElements, windowNanos, state));
                    case VStream.Done<A> ignored -> new VStream.Done<>();
                });
            }

            @Override
            public Task<Unit> close() {
                return stream.close();
            }
        };
    }

    private static void awaitPermit(int maxElements, long windowNanos, AtomicReference<WindowState> state) {
        while (true) {
            WindowState current = state.get();
            long now = System.nanoTime();
            long elapsed = now - current.windowStart();
            if (elapsed >= windowNanos) {
                WindowState next = new WindowState(now, 1);
                if (state.compareAndSet(current, next)) {
                    return;
                }
            } else if (current.emitted() >= maxElements) {
                sleep(Duration.ofNanos(windowNanos - elapsed));
            } else {
                WindowState next = new WindowState(current.windowStart(), current.emitted() + 1);
                if (state.compareAndSet(current, next)) {
                    return;
                }
            }
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interrupted);
        }
    }

    private record WindowState(long windowStart, int emitted) {
    }
}

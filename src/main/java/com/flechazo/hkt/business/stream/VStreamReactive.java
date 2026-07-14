package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.VTask;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class VStreamReactive {
    private static final int DEFAULT_BUFFER_SIZE = 256;

    private VStreamReactive() {
    }

    public static <A> Flow.Publisher<A> toPublisher(VStream<A> stream) {
        Objects.requireNonNull(stream, "stream");
        return subscriber -> {
            Objects.requireNonNull(subscriber, "subscriber");
            subscriber.onSubscribe(new VStreamSubscription<>(stream, subscriber));
        };
    }

    public static <A> VStream<A> fromPublisher(Flow.Publisher<A> publisher) {
        return fromPublisher(publisher, DEFAULT_BUFFER_SIZE);
    }

    public static <A> VStream<A> fromPublisher(Flow.Publisher<A> publisher, int bufferSize) {
        Objects.requireNonNull(publisher, "publisher");
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive");
        }

        LinkedBlockingQueue<Signal<A>> queue = new LinkedBlockingQueue<>(bufferSize);
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        QueueSubscriber<A> subscriber = new QueueSubscriber<>(queue, bufferSize, closed, completed);
        publisher.subscribe(subscriber);

        return new VStream<>() {
            @Override
            public VTask<Step<A>> pull() {
                return VTask.delay(() -> {
                    Signal<A> signal = queue.take();
                    return switch (signal) {
                        case Element<A> element -> new Emit<>(element.value(), this);
                        case Complete<A> ignored -> new Done<>();
                        case Failure<A> failure -> throw rethrow(failure.error());
                    };
                });
            }

            @Override
            public VTask<Unit> close() {
                return VTask.delay(() -> {
                    closed.set(true);
                    subscriber.cancel();
                    if (completed.compareAndSet(false, true)) {
                        queue.offer(new Complete<>());
                    }
                    return Unit.INSTANCE;
                });
            }
        };
    }

    private sealed interface Signal<A> permits Element, Complete, Failure {
    }

    private record Element<A>(A value) implements Signal<A> {
        private Element {
            Objects.requireNonNull(value, "value");
        }
    }

    private record Complete<A>() implements Signal<A> {
    }

    private record Failure<A>(Throwable error) implements Signal<A> {
        private Failure {
            Objects.requireNonNull(error, "error");
        }
    }

    private static final class QueueSubscriber<A> implements Flow.Subscriber<A> {
        private final LinkedBlockingQueue<Signal<A>> queue;
        private final int bufferSize;
        private final AtomicBoolean closed;
        private final AtomicBoolean completed;
        private volatile Flow.Subscription subscription;

        private QueueSubscriber(
                LinkedBlockingQueue<Signal<A>> queue,
                int bufferSize,
                AtomicBoolean closed,
                AtomicBoolean completed) {
            this.queue = queue;
            this.bufferSize = bufferSize;
            this.closed = closed;
            this.completed = completed;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = Objects.requireNonNull(subscription, "subscription");
            subscription.request(bufferSize);
        }

        @Override
        public void onNext(A item) {
            if (closed.get()) {
                cancel();
                return;
            }
            try {
                queue.put(new Element<>(item));
                subscription.request(1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                cancel();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (completed.compareAndSet(false, true)) {
                enqueueTerminal(new Failure<>(throwable));
            }
        }

        @Override
        public void onComplete() {
            if (completed.compareAndSet(false, true)) {
                enqueueTerminal(new Complete<>());
            }
        }

        private void cancel() {
            Flow.Subscription current = subscription;
            if (current != null) {
                current.cancel();
            }
        }

        private void enqueueTerminal(Signal<A> signal) {
            try {
                queue.put(signal);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                cancel();
            }
        }
    }

    private static final class VStreamSubscription<A> implements Flow.Subscription {
        private volatile VStream<A> current;
        private final Flow.Subscriber<? super A> subscriber;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean draining = new AtomicBoolean();

        private VStreamSubscription(VStream<A> stream, Flow.Subscriber<? super A> subscriber) {
            this.current = stream;
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                cancel();
                subscriber.onError(new IllegalArgumentException("request must be positive"));
                return;
            }
            demand.getAndAccumulate(n, VStreamReactive::addDemand);
            drain();
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                current.close().unsafeRun();
            }
        }

        private void drain() {
            if (!draining.compareAndSet(false, true)) {
                return;
            }
            Thread.ofVirtual().start(() -> {
                try {
                    while (!cancelled.get() && demand.get() > 0) {
                        VStream.Step<A> step = current.pull().unsafeRun();
                        switch (step) {
                            case VStream.Emit<A> emit -> {
                                current = emit.tail();
                                demand.decrementAndGet();
                                subscriber.onNext(emit.value());
                            }
                            case VStream.Skip<A> skip -> current = skip.tail();
                            case VStream.Done<A> ignored -> {
                                cancelled.set(true);
                                subscriber.onComplete();
                                return;
                            }
                        }
                    }
                    while (!cancelled.get() && demand.get() == 0) {
                        VStream.Step<A> step = current.pull().unsafeRun();
                        switch (step) {
                            case VStream.Done<A> ignored -> {
                                cancelled.set(true);
                                subscriber.onComplete();
                                return;
                            }
                            case VStream.Skip<A> skip -> current = skip.tail();
                            case VStream.Emit<A> emit -> {
                                current = VStream.concat(VStream.of(emit.value()), emit.tail());
                                return;
                            }
                        }
                    }
                } catch (Throwable error) {
                    if (!cancelled.getAndSet(true)) {
                        subscriber.onError(error);
                    }
                } finally {
                    draining.set(false);
                    if (!cancelled.get() && demand.get() > 0) {
                        drain();
                    }
                }
            });
        }
    }

    private static long addDemand(long current, long requested) {
        long next = current + requested;
        return next < 0 ? Long.MAX_VALUE : next;
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

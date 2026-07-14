package com.flechazo.hkt.business.effect;


import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.Effectful;
import com.flechazo.hkt.business.capability.combinable.IOCombinable;
import com.flechazo.hkt.business.control.TryPath;
import com.flechazo.hkt.business.resilience.Bulkhead;
import com.flechazo.hkt.business.resilience.CircuitBreaker;
import com.flechazo.hkt.business.resilience.RetryPolicy;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class IOPath<A> implements Effectful<A>, IOCombinable<A> {
    private final IO<A> value;

    public IOPath(IO<A> value) {
        this.value = value;
    }

    public IO<A> run() {
        return value;
    }

    @Override
    public A unsafeRun() {
        try {
            return value.unsafeRun();
        } catch (RuntimeException runtime) {
            throw runtime;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Try<A> runSafe() {
        return Try.of(value::unsafeRun);
    }

    public TryPath<A> toTryPath() {
        return new TryPath<>(runSafe());
    }

    public IOPath<Unit> asUnit() {
        return new IOPath<>(value.asUnit());
    }

    @Override
    public <B> IOPath<B> map(Function<? super A, ? extends B> mapper) {
        return new IOPath<>(value.map(mapper));
    }

    @Override
    public IOPath<A> peek(Consumer<? super A> consumer) {
        return new IOPath<>(value.peek(consumer));
    }

    @Override
    public <B, C> IOPath<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof IOPath<?> otherIO)) {
            throw new IllegalArgumentException("Cannot zipWith non-IOPath: " + other.getClass());
        }
        IOPath<B> typedOther = (IOPath<B>) otherIO;
        return new IOPath<>(value.flatMap(left -> typedOther.value.map(right -> combiner.apply(left, right))));
    }

    @Override
    public <B> IOPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        return new IOPath<>(value.flatMap(a -> {
            Chainable<B> result = mapper.apply(a);
            if (!(result instanceof IOPath<?> IOPath)) {
                throw new IllegalArgumentException("via mapper must return IOPath, got: " + result.getClass());
            }
            return ((IOPath<B>) IOPath).value;
        }));
    }

    @Override
    public <B> IOPath<B> flatMap(Function<? super A, ? extends Chainable<B>> mapper) {
        return via(mapper);
    }

    @Override
    public <B> IOPath<B> then(Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    @Override
    public IOPath<A> handleError(Function<? super Throwable, ? extends A> recovery) {
        return new IOPath<>(value.recover(recovery));
    }

    @Override
    public IOPath<A> handleErrorWith(Function<? super Throwable, ? extends Effectful<A>> recovery) {
        return new IOPath<>(value.recoverWith(error -> {
            Effectful<A> result = recovery.apply(error);
            if (!(result instanceof IOPath<?> IOPath)) {
                throw new IllegalArgumentException("recovery must return IOPath, got: " + result.getClass());
            }
            return ((IOPath<A>) IOPath).value;
        }));
    }

    @Override
    public IOPath<A> guarantee(Runnable finalizer) {
        return new IOPath<>(() -> {
            try {
                return value.unsafeRun();
            } finally {
                finalizer.run();
            }
        });
    }

    public IOPath<A> guarantee(IOPath<Unit> finalizer) {
        return new IOPath<>(value.guarantee(finalizer.run()));
    }

    public IOResourcePath<A> asResource(Function<? super A, IOPath<Unit>> release) {
        return new IOResourcePath<>(value.asResource(resource -> release.apply(resource).run()));
    }

    public IOPath<Maybe<A>> asMaybe() {
        return new IOPath<>(() -> runSafe().toMaybe());
    }

    public IOPath<Try<A>> asTry() {
        return new IOPath<>(this::runSafe);
    }

    public IOPath<A> retry(RetryPolicy policy) {
        return new IOPath<>(value.retry(policy));
    }

    public IOPath<A> circuitBreaker(CircuitBreaker circuitBreaker) {
        return new IOPath<>(value.circuitBreaker(circuitBreaker));
    }

    public IOPath<A> bulkhead(Bulkhead bulkhead) {
        return new IOPath<>(value.bulkhead(bulkhead));
    }

}

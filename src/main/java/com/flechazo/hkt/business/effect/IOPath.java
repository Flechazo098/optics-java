package com.flechazo.hkt.business.effect;


import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Effectful;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.IOCombinable;
import com.flechazo.hkt.business.control.TryPath;
import com.flechazo.hkt.business.resilience.Bulkhead;
import com.flechazo.hkt.business.resilience.CircuitBreaker;
import com.flechazo.hkt.business.resilience.RetryPolicy;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides fluent composition for synchronous IO computations.
 *
 * @param <A> the result type
 */
public final class IOPath<A> implements Effectful<A>, IOCombinable<A> {
    private final IO<A> value;

    /**
     * Creates a path over an IO computation.
     *
     * @param value the IO computation
     */
    public IOPath(IO<A> value) {
        this.value = value;
    }

    /**
     * Returns the underlying IO computation.
     *
     * @return the IO computation represented by this path
     */
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

    /**
     * Executes this path and captures its outcome in a try path.
     *
     * @return a try path containing the result or failure
     */
    public TryPath<A> toTryPath() {
        return new TryPath<>(runSafe());
    }

    /**
     * Replaces the successful result with {@link Unit}.
     *
     * @return an IO path producing {@link Unit}
     */
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

    /**
     * Runs two IO paths in order and combines their successful results.
     *
     * @param <B> the other result type
     * @param <C> the combined result type
     * @param other the IO path to combine with this path
     * @param combiner the function combining both results
     * @return the combined IO path
     * @throws IllegalArgumentException if {@code other} is not an IO path
     */
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

    /**
     * Registers a finalizer that executes after success or failure.
     *
     * @param finalizer the completion action
     * @return a path with the finalizer attached
     */
    public IOPath<A> guarantee(IOPath<Unit> finalizer) {
        return new IOPath<>(value.guarantee(finalizer.run()));
    }

    /**
     * Converts this path to a managed resource.
     *
     * @param release the release path selected from the acquired value
     * @return a managed IO resource path
     */
    public IOResourcePath<A> asResource(Function<? super A, IOPath<Unit>> release) {
        return new IOResourcePath<>(value.asResource(resource -> release.apply(resource).run()));
    }

    /**
     * Captures failure as absence in the result value.
     *
     * @return an IO path producing a defined result or an empty value
     */
    public IOPath<Maybe<A>> asMaybe() {
        return new IOPath<>(() -> runSafe().toMaybe());
    }

    /**
     * Captures execution outcome in the result value.
     *
     * @return an IO path producing a successful or failed try value
     */
    public IOPath<Try<A>> asTry() {
        return new IOPath<>(this::runSafe);
    }

    /**
     * Retries failures according to a policy.
     *
     * @param policy the retry policy
     * @return a path protected by the retry policy
     */
    public IOPath<A> retry(RetryPolicy policy) {
        return new IOPath<>(value.retry(policy));
    }

    /**
     * Protects execution with a circuit breaker.
     *
     * @param circuitBreaker the circuit breaker
     * @return the protected path
     */
    public IOPath<A> circuitBreaker(CircuitBreaker circuitBreaker) {
        return new IOPath<>(value.circuitBreaker(circuitBreaker));
    }

    /**
     * Protects execution with a bulkhead.
     *
     * @param bulkhead the bulkhead
     * @return the protected path
     */
    public IOPath<A> bulkhead(Bulkhead bulkhead) {
        return new IOPath<>(value.bulkhead(bulkhead));
    }

}

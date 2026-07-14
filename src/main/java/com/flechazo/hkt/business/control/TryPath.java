package com.flechazo.hkt.business.control;


import com.flechazo.hkt.Semigroup;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Recoverable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.TryCombinable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides fluent composition for computations that capture throwable failures.
 *
 * @param <A> the result type
 */
public final class TryPath<A> implements Recoverable<Throwable, A>, TryCombinable<A> {
    private final Try<A> value;

    /**
     * Creates a path over a try value.
     *
     * @param value the try value
     */
    public TryPath(Try<A> value) {
        this.value = value;
    }

    /**
     * Returns the underlying try value.
     *
     * @return the try represented by this path
     */
    public Try<A> run() {
        return value;
    }

    /**
     * Returns the successful value or a fallback.
     *
     * @param defaultValue the value returned on failure
     * @return the successful value or {@code defaultValue}
     */
    public A getOrElse(A defaultValue) {
        return value.orElse(defaultValue);
    }

    /**
     * Returns the successful value or obtains a fallback lazily.
     *
     * @param supplier the supplier invoked on failure
     * @return the successful or supplied value
     */
    public A getOrElseGet(Supplier<? extends A> supplier) {
        return value.orElseGet(supplier);
    }

    /**
     * Discards failure details and preserves a successful value when present.
     *
     * @return a maybe path containing the successful value, or an empty path
     */
    public MaybePath<A> toMaybePath() {
        return new MaybePath<>(value.toMaybe());
    }

    /**
     * Converts this path to an either path retaining the failure cause.
     *
     * @return a left failure or right successful value
     */
    public EitherPath<Throwable, A> toEitherPath() {
        return new EitherPath<>(value.toEither());
    }

    /**
     * Converts this path to an either path with a mapped failure.
     *
     * @param <E> the error type
     * @param failureToLeft the function converting a failure to a left value
     * @return a mapped left error or right successful value
     */
    public <E> EitherPath<E, A> toEitherPath(Function<? super Throwable, ? extends E> failureToLeft) {
        return new EitherPath<>(value.toEither(failureToLeft));
    }

    /**
     * Converts this path to a validation path with a mapped failure.
     *
     * @param <E> the validation error type
     * @param failureToInvalid the function converting a failure to an invalid error
     * @param semigroup the operation used to accumulate validation errors
     * @return an invalid error or valid successful value
     */
    public <E> ValidationPath<E, A> toValidationPath(
            Function<? super Throwable, ? extends E> failureToInvalid,
            Semigroup<E> semigroup) {
        return new ValidationPath<>(value.toValidated(failureToInvalid), semigroup);
    }

    @Override
    public <B> TryPath<B> map(Function<? super A, ? extends B> mapper) {
        return new TryPath<>(value.map(mapper));
    }

    @Override
    public TryPath<A> peek(Consumer<? super A> consumer) {
        value.peek(consumer);
        return this;
    }

    /**
     * Observes a failure while preserving this path.
     *
     * @param consumer the operation invoked for a failure
     * @return a path preserving the original try value
     */
    public TryPath<A> peekFailure(Consumer<? super Throwable> consumer) {
        value.peekFailure(consumer);
        return this;
    }

    /**
     * Combines two successful values and propagates the first failure.
     *
     * @param <B> the other success value type
     * @param <C> the combined success value type
     * @param other the try path to combine with this path
     * @param combiner the function combining both successful values
     * @return the combined try path
     * @throws IllegalArgumentException if {@code other} is not a try path
     */
    @Override
    public <B, C> TryPath<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof TryPath<?> otherTry)) {
            throw new IllegalArgumentException("Cannot zipWith non-TryPath: " + other.getClass());
        }
        if (value.isFailure()) {
            return new TryPath<>(Try.failure(value.cause()));
        }
        TryPath<B> typedOther = (TryPath<B>) otherTry;
        if (typedOther.value.isFailure()) {
            return new TryPath<>(Try.failure(typedOther.value.cause()));
        }
        return new TryPath<>(Try.of(() -> combiner.apply(value.get(), typedOther.value.get())));
    }

    @Override
    public <B> TryPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        if (value.isFailure()) {
            return new TryPath<>(Try.failure(value.cause()));
        }
        Chainable<B> result = mapper.apply(value.get());
        if (!(result instanceof TryPath<?> tryPath)) {
            throw new IllegalArgumentException("via mapper must return TryPath, got: " + result.getClass());
        }
        return (TryPath<B>) tryPath;
    }

    @Override
    public <B> TryPath<B> then(Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    @Override
    public TryPath<A> recover(Function<? super Throwable, ? extends A> recovery) {
        return new TryPath<>(value.recover(recovery));
    }

    @Override
    public TryPath<A> recoverWith(Function<? super Throwable, ? extends Recoverable<Throwable, A>> recovery) {
        if (value.isSuccess()) {
            return this;
        }
        Recoverable<Throwable, A> result = recovery.apply(value.cause());
        if (!(result instanceof TryPath<?> tryPath)) {
            throw new IllegalArgumentException("recovery must return TryPath, got: " + result.getClass());
        }
        return (TryPath<A>) tryPath;
    }

    @Override
    public TryPath<A> orElse(Supplier<? extends Recoverable<Throwable, A>> alternative) {
        return recoverWith(ignored -> alternative.get());
    }

    @Override
    public <E2> Recoverable<E2, A> mapError(Function<? super Throwable, ? extends E2> mapper) {
        return new EitherPath<>(value.toEither(mapper));
    }

    /**
     * Transforms a failure cause while preserving a successful value.
     *
     * @param mapper the failure transformation
     * @return a try path with the transformed failure
     */
    public TryPath<A> mapException(Function<? super Throwable, ? extends Throwable> mapper) {
        return new TryPath<>(value.mapError(mapper));
    }

}

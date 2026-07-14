package com.flechazo.hkt.business.control;


import com.flechazo.hkt.Semigroup;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.business.capability.Accumulating;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Recoverable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.ValidationCombinable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides fluent composition for validations that can accumulate errors.
 *
 * @param <E> the validation error type
 * @param <A> the valid value type
 */
public final class ValidationPath<E, A>
        implements Recoverable<E, A>, ValidationAccumulating<E, A>, ValidationCombinable<E, A> {
    private final Validated<E, A> value;
    private final Semigroup<E> semigroup;

    /**
     * Creates a path over a validation and its error semigroup.
     *
     * @param value the validation value
     * @param semigroup the operation used to accumulate errors
     */
    public ValidationPath(Validated<E, A> value, Semigroup<E> semigroup) {
        this.value = value;
        this.semigroup = semigroup;
    }

    /**
     * Returns the underlying validation.
     *
     * @return the validation represented by this path
     */
    public Validated<E, A> run() {
        return value;
    }

    /**
     * Returns the semigroup used to accumulate errors.
     *
     * @return the error semigroup
     */
    public Semigroup<E> semigroup() {
        return semigroup;
    }

    /**
     * Returns the valid value or a fallback.
     *
     * @param defaultValue the value returned for an invalid result
     * @return the valid value or {@code defaultValue}
     */
    public A getOrElse(A defaultValue) {
        return value.isValid() ? value.value() : defaultValue;
    }

    /**
     * Returns the valid value or obtains a fallback lazily.
     *
     * @param supplier the supplier invoked for an invalid result
     * @return the valid or supplied value
     */
    public A getOrElseGet(Supplier<? extends A> supplier) {
        return value.isValid() ? value.value() : supplier.get();
    }

    /**
     * Folds either alternative into a common result type.
     *
     * @param <B> the result type
     * @param invalid the function applied to an invalid error
     * @param valid the function applied to a valid value
     * @return the folded result
     */
    public <B> B fold(Function<? super E, ? extends B> invalid, Function<? super A, ? extends B> valid) {
        return value.fold(invalid, valid);
    }

    /**
     * Determines whether this path contains a valid value.
     *
     * @return {@code true} for a valid value
     */
    public boolean isValid() {
        return value.isValid();
    }

    /**
     * Determines whether this path contains an invalid error.
     *
     * @return {@code true} for an invalid value
     */
    public boolean isInvalid() {
        return value.isInvalid();
    }

    /**
     * Converts this path to an either path without changing alternatives.
     *
     * @return a left error or right valid value
     */
    public EitherPath<E, A> toEitherPath() {
        return new EitherPath<>(value.toEither());
    }

    /**
     * Discards invalid errors and preserves a valid value when present.
     *
     * @return a maybe path containing the valid value, or an empty path
     */
    public MaybePath<A> toMaybePath() {
        return new MaybePath<>(value.toMaybe());
    }

    /**
     * Converts this path to a try path with a throwable mapped from an invalid error.
     *
     * @param errorToException the function converting an invalid error to a failure cause
     * @return a failed or successful try path
     */
    public TryPath<A> toTryPath(Function<? super E, ? extends Throwable> errorToException) {
        return new TryPath<>(value.toTry(errorToException));
    }

    @Override
    public <B> ValidationPath<E, B> map(Function<? super A, ? extends B> mapper) {
        return new ValidationPath<>(value.map(mapper), semigroup);
    }

    @Override
    public ValidationPath<E, A> peek(Consumer<? super A> consumer) {
        value.peek(consumer);
        return this;
    }

    /**
     * Observes an invalid error while preserving this path.
     *
     * @param consumer the operation invoked for an invalid error
     * @return a path preserving the original validation
     */
    public ValidationPath<E, A> peekInvalid(Consumer<? super E> consumer) {
        value.peekInvalid(consumer);
        return this;
    }

    /**
     * Combines two valid values and accumulates errors from invalid inputs.
     *
     * @param <B> the other valid value type
     * @param <C> the combined valid value type
     * @param other the validation path to combine with this path
     * @param combiner the function combining both valid values
     * @return the combined validation path
     * @throws IllegalArgumentException if {@code other} is not a validation path
     */
    @Override
    public <B, C> ValidationPath<E, C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof ValidationPath<?, ?> otherValidation)) {
            throw new IllegalArgumentException("Cannot zipWith non-ValidationPath: " + other.getClass());
        }
        ValidationPath<E, B> typedOther = (ValidationPath<E, B>) otherValidation;
        if (value.isValid() && typedOther.value.isValid()) {
            return new ValidationPath<>(Validated.valid(combiner.apply(value.value(), typedOther.value.value())), semigroup);
        }
        if (value.isInvalid()) {
            return new ValidationPath<>(Validated.invalid(value.error()), semigroup);
        }
        return new ValidationPath<>(Validated.invalid(typedOther.value.error()), semigroup);
    }

    /**
     * Accumulates errors from another validation and preserves this valid value when both are valid.
     *
     * @param other the validation combined with this path
     * @return a path containing accumulated errors or this valid value
     */
    @Override
    public ValidationPath<E, A> andAlso(Accumulating<E, ?> other) {
        if (!(other instanceof ValidationPath<?, ?> otherValidation)) {
            throw new IllegalArgumentException("andAlso requires ValidationPath, got: " + other.getClass());
        }
        return zipWithAccum((ValidationPath<E, Object>) otherValidation, (left, ignored) -> left);
    }

    /**
     * Accumulates errors from another validation and returns its value when both are valid.
     *
     * @param <B> the next valid value type
     * @param other the validation combined with this path
     * @return a path containing accumulated errors or the other valid value
     */
    @Override
    public <B> ValidationPath<E, B> andThen(Accumulating<E, B> other) {
        return zipWithAccum(other, (ignored, right) -> right);
    }

    @Override
    public <B> ValidationPath<E, B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        if (value.isInvalid()) {
            return new ValidationPath<>(Validated.invalid(value.error()), semigroup);
        }
        Chainable<B> result = mapper.apply(value.value());
        if (!(result instanceof ValidationPath<?, ?> validationPath)) {
            throw new IllegalArgumentException("via mapper must return ValidationPath, got: " + result.getClass());
        }
        return (ValidationPath<E, B>) validationPath;
    }

    @Override
    public <B> ValidationPath<E, B> then(Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    @Override
    public ValidationPath<E, A> recover(Function<? super E, ? extends A> recovery) {
        return value.isValid() ? this : new ValidationPath<>(Validated.valid(recovery.apply(value.error())), semigroup);
    }

    @Override
    public ValidationPath<E, A> recoverWith(Function<? super E, ? extends Recoverable<E, A>> recovery) {
        if (value.isValid()) {
            return this;
        }
        Recoverable<E, A> result = recovery.apply(value.error());
        if (!(result instanceof ValidationPath<?, ?> validationPath)) {
            throw new IllegalArgumentException("recovery must return ValidationPath, got: " + result.getClass());
        }
        return (ValidationPath<E, A>) validationPath;
    }

    @Override
    public ValidationPath<E, A> orElse(Supplier<? extends Recoverable<E, A>> alternative) {
        if (value.isValid()) {
            return this;
        }
        Recoverable<E, A> result = alternative.get();
        if (!(result instanceof ValidationPath<?, ?> validationPath)) {
            throw new IllegalArgumentException("alternative must return ValidationPath, got: " + result.getClass());
        }
        return (ValidationPath<E, A>) validationPath;
    }

    @Override
    public <E2> Recoverable<E2, A> mapError(Function<? super E, ? extends E2> mapper) {
        return toEitherPath().mapError(mapper);
    }

    /**
     * Transforms the error type and installs its accumulation operation.
     *
     * @param <E2> the transformed error type
     * @param mapper the error transformation
     * @param nextSemigroup the operation used to accumulate transformed errors
     * @return a validation path with the transformed error channel
     */
    public <E2> ValidationPath<E2, A> mapError(Function<? super E, ? extends E2> mapper, Semigroup<E2> nextSemigroup) {
        return new ValidationPath<>(value.mapError(mapper), nextSemigroup);
    }

}

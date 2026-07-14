package com.flechazo.hkt.business.control;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Recoverable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.MaybeCombinable;
import com.flechazo.hkt.business.core.Pathway;

import java.util.Objects;
import java.util.function.*;

/**
 * Provides fluent composition for computations that may not produce a value.
 *
 * @param <A> the result type
 */
public final class MaybePath<A> implements Recoverable<Unit, A>, MaybeCombinable<A> {
    private final Maybe<A> value;

    /**
     * Creates a path over a maybe value.
     *
     * @param value the maybe value
     */
    public MaybePath(Maybe<A> value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    /**
     * Returns the underlying maybe value.
     *
     * @return the maybe value represented by this path
     */
    public Maybe<A> run() {
        return value;
    }

    /**
     * Returns the contained value or a fallback.
     *
     * @param fallback the value returned when this path is empty
     * @return the contained value or {@code fallback}
     */
    public A getOrElse(A fallback) {
        return value.orElse(fallback);
    }

    /**
     * Returns the contained value or obtains a fallback lazily.
     *
     * @param fallback the supplier invoked when this path is empty
     * @return the contained or supplied value
     */
    public A getOrElseGet(Supplier<? extends A> fallback) {
        return value.orElseGet(fallback);
    }

    /**
     * Retains the contained value only when it satisfies a predicate.
     *
     * @param predicate the condition for retaining the value
     * @return this value when it matches, otherwise an empty path
     */
    public MaybePath<A> filter(Predicate<? super A> predicate) {
        return new MaybePath<>(value.filter(predicate));
    }

    @Override
    public <B> MaybePath<B> map(Function<? super A, ? extends B> mapper) {
        return new MaybePath<>(value.map(mapper));
    }

    @Override
    public <B> MaybePath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (value.isEmpty()) {
            return Pathway.nothing();
        }
        Chainable<B> result = mapper.apply(value.get());
        if (!(result instanceof MaybePath<?> maybePath)) {
            throw new IllegalArgumentException("via mapper must return MaybePath, got: " + result.getClass());
        }
        return (MaybePath<B>) maybePath;
    }

    @Override
    public <B> MaybePath<B> then(Supplier<? extends Chainable<B>> supplier) {
        if (value.isEmpty()) {
            return Pathway.nothing();
        }
        Chainable<B> result = supplier.get();
        if (!(result instanceof MaybePath<?> maybePath)) {
            throw new IllegalArgumentException("then supplier must return MaybePath, got: " + result.getClass());
        }
        return (MaybePath<B>) maybePath;
    }

    /**
     * Combines two defined values and propagates absence from either path.
     *
     * @param <B> the other value type
     * @param <C> the combined value type
     * @param other the maybe path to combine with this path
     * @param combiner the function combining both defined values
     * @return the combined maybe path
     * @throws IllegalArgumentException if {@code other} is not a maybe path
     */
    @Override
    public <B, C> MaybePath<C> zipWith(Combinable<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combiner, "combiner");
        if (!(other instanceof MaybePath<?> otherMaybe)) {
            throw new IllegalArgumentException("Cannot zipWith non-MaybePath: " + other.getClass());
        }
        MaybePath<B> typedOther = (MaybePath<B>) otherMaybe;
        if (value.isEmpty() || typedOther.value.isEmpty()) {
            return Pathway.nothing();
        }
        return Pathway.just(combiner.apply(value.get(), typedOther.value.get()));
    }

    @Override
    public MaybePath<A> peek(Consumer<? super A> action) {
        value.peek(action);
        return this;
    }

    @Override
    public MaybePath<A> recover(Function<? super Unit, ? extends A> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        return value.isDefined() ? this : Pathway.just(recovery.apply(Unit.INSTANCE));
    }

    @Override
    public MaybePath<A> recoverWith(Function<? super Unit, ? extends Recoverable<Unit, A>> recovery) {
        if (value.isDefined()) {
            return this;
        }
        Recoverable<Unit, A> result = recovery.apply(Unit.INSTANCE);
        if (!(result instanceof MaybePath<?> maybePath)) {
            throw new IllegalArgumentException("recovery must return MaybePath, got: " + result.getClass());
        }
        return (MaybePath<A>) maybePath;
    }

    @Override
    public MaybePath<A> orElse(Supplier<? extends Recoverable<Unit, A>> alternative) {
        Objects.requireNonNull(alternative, "alternative");
        if (value.isDefined()) {
            return this;
        }
        Recoverable<Unit, A> result = alternative.get();
        if (!(result instanceof MaybePath<?> maybePath)) {
            throw new IllegalArgumentException("alternative must return MaybePath, got: " + result.getClass());
        }
        return (MaybePath<A>) maybePath;
    }

    @Override
    public <E2> Recoverable<E2, A> mapError(Function<? super Unit, ? extends E2> mapper) {
        return new EitherPath<>(value.toEither(() -> mapper.apply(Unit.INSTANCE)));
    }

    /**
     * Converts this path to an either value with a supplied error for absence.
     *
     * @param <E> the error type
     * @param ifEmpty the supplier invoked when this path is empty
     * @return a right value when defined, otherwise the supplied left error
     */
    public <E> Either<E, A> toEither(Supplier<? extends E> ifEmpty) {
        return value.toEither(ifEmpty);
    }

    /**
     * Converts this path to a validation with a supplied error for absence.
     *
     * @param <E> the error type
     * @param ifEmpty the supplier invoked when this path is empty
     * @return a valid value when defined, otherwise the supplied invalid error
     */
    public <E> Validated<E, A> toValidated(Supplier<? extends E> ifEmpty) {
        return value.toValidated(ifEmpty);
    }

}

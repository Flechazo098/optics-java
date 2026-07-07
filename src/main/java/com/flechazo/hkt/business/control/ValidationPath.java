package com.flechazo.hkt.business.control;

import com.flechazo.hkt.Semigroup;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.business.capability.Accumulating;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.business.capability.Recoverable;
import com.flechazo.hkt.function.Function3;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ValidationPath<E, A> implements Recoverable<E, A>, Accumulating<E, A> {
    private final Validated<E, A> value;
    private final Semigroup<E> semigroup;

    public ValidationPath(Validated<E, A> value, Semigroup<E> semigroup) {
        this.value = value;
        this.semigroup = semigroup;
    }

    public Validated<E, A> run() {
        return value;
    }

    public Semigroup<E> semigroup() {
        return semigroup;
    }

    public A getOrElse(A defaultValue) {
        return value.isValid() ? value.value() : defaultValue;
    }

    public A getOrElseGet(Supplier<? extends A> supplier) {
        return value.isValid() ? value.value() : supplier.get();
    }

    public <B> B fold(Function<? super E, ? extends B> invalid, Function<? super A, ? extends B> valid) {
        return value.fold(invalid, valid);
    }

    public boolean isValid() {
        return value.isValid();
    }

    public boolean isInvalid() {
        return value.isInvalid();
    }

    public EitherPath<E, A> toEitherPath() {
        return new EitherPath<>(value.toEither());
    }

    public MaybePath<A> toMaybePath() {
        return new MaybePath<>(value.toMaybe());
    }

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

    public ValidationPath<E, A> peekInvalid(Consumer<? super E> consumer) {
        value.peekInvalid(consumer);
        return this;
    }

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

    @Override
    public <B, C, D> ValidationPath<E, D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWith(second, Combinable.Pair2::new)
                .zipWith(third, (pair, c) -> combiner.apply(pair.first(), pair.second(), c));
    }

    @Override
    public <B, C> ValidationPath<E, C> zipWithAccum(
            Accumulating<E, B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof ValidationPath<?, ?> otherValidation)) {
            throw new IllegalArgumentException("zipWithAccum requires ValidationPath, got: " + other.getClass());
        }
        ValidationPath<E, B> typedOther = (ValidationPath<E, B>) otherValidation;
        if (value.isValid() && typedOther.value.isValid()) {
            return new ValidationPath<>(Validated.valid(combiner.apply(value.value(), typedOther.value.value())), semigroup);
        }
        if (value.isInvalid() && typedOther.value.isInvalid()) {
            return new ValidationPath<>(Validated.invalid(semigroup.combine(value.error(), typedOther.value.error())), semigroup);
        }
        return value.isInvalid()
                ? new ValidationPath<>(Validated.invalid(value.error()), semigroup)
                : new ValidationPath<>(Validated.invalid(typedOther.value.error()), semigroup);
    }

    @Override
    public <B, C, D> ValidationPath<E, D> zipWith3Accum(
            Accumulating<E, B> second,
            Accumulating<E, C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWithAccum(second, Combinable.Pair2::new)
                .zipWithAccum(third, (pair, c) -> combiner.apply(pair.first(), pair.second(), c));
    }

    @Override
    public ValidationPath<E, A> andAlso(Accumulating<E, ?> other) {
        if (!(other instanceof ValidationPath<?, ?> otherValidation)) {
            throw new IllegalArgumentException("andAlso requires ValidationPath, got: " + other.getClass());
        }
        return zipWithAccum((ValidationPath<E, Object>) otherValidation, (left, ignored) -> left);
    }

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

    public <E2> ValidationPath<E2, A> mapError(Function<? super E, ? extends E2> mapper, Semigroup<E2> nextSemigroup) {
        return new ValidationPath<>(value.mapError(mapper), nextSemigroup);
    }

}

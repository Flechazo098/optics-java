package com.flechazo.hkt.business.control;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.Recoverable;
import com.flechazo.hkt.business.capability.combinable.EitherCombinable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class EitherPath<E, A> implements Recoverable<E, A>, EitherCombinable<E, A> {
    private final Either<E, A> value;

    public EitherPath(Either<E, A> value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public Either<E, A> run() {
        return value;
    }

    public A getOrElse(A fallback) {
        return value.getOrElse(fallback);
    }

    public A getOrElseGet(Supplier<? extends A> fallback) {
        return value.getOrElseGet(fallback);
    }

    @Override
    public <B> EitherPath<E, B> map(Function<? super A, ? extends B> mapper) {
        return new EitherPath<>(value.map(mapper));
    }

    public <E2> EitherPath<E2, A> mapError(Function<? super E, ? extends E2> mapper) {
        return new EitherPath<>(value.mapLeft(mapper));
    }

    @Override
    public <B> EitherPath<E, B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (value.isLeft()) {
            return new EitherPath<>(Either.left(value.left()));
        }
        Chainable<B> result = mapper.apply(value.right());
        if (!(result instanceof EitherPath<?, ?> eitherPath)) {
            throw new IllegalArgumentException("via mapper must return EitherPath, got: " + result.getClass());
        }
        return (EitherPath<E, B>) eitherPath;
    }

    @Override
    public <B> EitherPath<E, B> then(Supplier<? extends Chainable<B>> supplier) {
        if (value.isLeft()) {
            return new EitherPath<>(Either.left(value.left()));
        }
        Chainable<B> result = supplier.get();
        if (!(result instanceof EitherPath<?, ?> eitherPath)) {
            throw new IllegalArgumentException("then supplier must return EitherPath, got: " + result.getClass());
        }
        return (EitherPath<E, B>) eitherPath;
    }

    @Override
    public <B, C> EitherPath<E, C> zipWith(Combinable<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combiner, "combiner");
        if (!(other instanceof EitherPath<?, ?> otherEither)) {
            throw new IllegalArgumentException("Cannot zipWith non-EitherPath: " + other.getClass());
        }
        EitherPath<E, B> typedOther = (EitherPath<E, B>) otherEither;
        if (value.isLeft()) {
            return new EitherPath<>(Either.left(value.left()));
        }
        if (typedOther.value.isLeft()) {
            return new EitherPath<>(Either.left(typedOther.value.left()));
        }
        return new EitherPath<>(Either.right(combiner.apply(value.right(), typedOther.value.right())));
    }

    @Override
    public EitherPath<E, A> peek(Consumer<? super A> action) {
        value.peek(action);
        return this;
    }

    public EitherPath<E, A> peekLeft(Consumer<? super E> action) {
        value.peekLeft(action);
        return this;
    }

    @Override
    public EitherPath<E, A> recover(Function<? super E, ? extends A> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        return value.isRight() ? this : new EitherPath<>(Either.right(recovery.apply(value.left())));
    }

    @Override
    public EitherPath<E, A> recoverWith(Function<? super E, ? extends Recoverable<E, A>> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        if (value.isRight()) {
            return this;
        }
        Recoverable<E, A> result = recovery.apply(value.left());
        if (!(result instanceof EitherPath<?, ?> eitherPath)) {
            throw new IllegalArgumentException("recovery must return EitherPath, got: " + result.getClass());
        }
        return (EitherPath<E, A>) eitherPath;
    }

    @Override
    public EitherPath<E, A> orElse(Supplier<? extends Recoverable<E, A>> alternative) {
        if (value.isRight()) {
            return this;
        }
        Recoverable<E, A> result = alternative.get();
        if (!(result instanceof EitherPath<?, ?> eitherPath)) {
            throw new IllegalArgumentException("alternative must return EitherPath, got: " + result.getClass());
        }
        return (EitherPath<E, A>) eitherPath;
    }

    public MaybePath<A> toMaybePath() {
        Maybe<A> maybe = value.toMaybe();
        return new MaybePath<>(maybe);
    }

    public Validated<E, A> toValidated() {
        return value.toValidated();
    }

}

package com.flechazo.hkt.business.control;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.business.capability.Recoverable;
import com.flechazo.hkt.business.core.Pathway;

import java.util.Objects;
import java.util.function.*;

public final class MaybePath<A> implements Recoverable<Unit, A> {
    private final Maybe<A> value;

    public MaybePath(Maybe<A> value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public Maybe<A> run() {
        return value;
    }

    public A getOrElse(A fallback) {
        return value.orElse(fallback);
    }

    public A getOrElseGet(Supplier<? extends A> fallback) {
        return value.orElseGet(fallback);
    }

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

    public <E> Either<E, A> toEither(Supplier<? extends E> ifEmpty) {
        return value.toEither(ifEmpty);
    }

    public <E> Validated<E, A> toValidated(Supplier<? extends E> ifEmpty) {
        return value.toValidated(ifEmpty);
    }

}

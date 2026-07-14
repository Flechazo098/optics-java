package com.flechazo.hkt.business.control;


import com.flechazo.hkt.Semigroup;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.Recoverable;
import com.flechazo.hkt.business.capability.combinable.TryCombinable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TryPath<A> implements Recoverable<Throwable, A>, TryCombinable<A> {
    private final Try<A> value;

    public TryPath(Try<A> value) {
        this.value = value;
    }

    public Try<A> run() {
        return value;
    }

    public A getOrElse(A defaultValue) {
        return value.orElse(defaultValue);
    }

    public A getOrElseGet(Supplier<? extends A> supplier) {
        return value.orElseGet(supplier);
    }

    public MaybePath<A> toMaybePath() {
        return new MaybePath<>(value.toMaybe());
    }

    public EitherPath<Throwable, A> toEitherPath() {
        return new EitherPath<>(value.toEither());
    }

    public <E> EitherPath<E, A> toEitherPath(Function<? super Throwable, ? extends E> failureToLeft) {
        return new EitherPath<>(value.toEither(failureToLeft));
    }

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

    public TryPath<A> peekFailure(Consumer<? super Throwable> consumer) {
        value.peekFailure(consumer);
        return this;
    }

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

    public TryPath<A> mapException(Function<? super Throwable, ? extends Throwable> mapper) {
        return new TryPath<>(value.mapError(mapper));
    }

}

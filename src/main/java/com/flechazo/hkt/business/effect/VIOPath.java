package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Tuple2;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.business.capability.Effectful;
import com.flechazo.hkt.business.control.TryPath;
import com.flechazo.hkt.function.Function3;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class VIOPath<A> implements Effectful<A> {
    private final VIO<A> value;

    public VIOPath(VIO<A> value) {
        this.value = value;
    }

    public VIO<A> run() {
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

    public VIOPath<Unit> asUnit() {
        return new VIOPath<>(value.asUnit());
    }

    @Override
    public <B> VIOPath<B> map(Function<? super A, ? extends B> mapper) {
        return new VIOPath<>(value.map(mapper));
    }

    @Override
    public VIOPath<A> peek(Consumer<? super A> consumer) {
        return new VIOPath<>(value.peek(consumer));
    }

    @Override
    public <B, C> VIOPath<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof VIOPath<?> otherIO)) {
            throw new IllegalArgumentException("Cannot zipWith non-VIOPath: " + other.getClass());
        }
        VIOPath<B> typedOther = (VIOPath<B>) otherIO;
        return new VIOPath<>(value.flatMap(left -> typedOther.value.map(right -> combiner.apply(left, right))));
    }

    @Override
    public <B, C, D> VIOPath<D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWith(second, Tuple2::new)
                .zipWith(third, (tuple, c) -> combiner.apply(tuple.first(), tuple.second(), c));
    }

    @Override
    public <B> VIOPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        return new VIOPath<>(value.flatMap(a -> {
            Chainable<B> result = mapper.apply(a);
            if (!(result instanceof VIOPath<?> VIOPath)) {
                throw new IllegalArgumentException("via mapper must return VIOPath, got: " + result.getClass());
            }
            return ((VIOPath<B>) VIOPath).value;
        }));
    }

    @Override
    public <B> VIOPath<B> flatMap(Function<? super A, ? extends Chainable<B>> mapper) {
        return via(mapper);
    }

    @Override
    public <B> VIOPath<B> then(Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    @Override
    public VIOPath<A> handleError(Function<? super Throwable, ? extends A> recovery) {
        return new VIOPath<>(value.recover(recovery));
    }

    @Override
    public VIOPath<A> handleErrorWith(Function<? super Throwable, ? extends Effectful<A>> recovery) {
        return new VIOPath<>(value.recoverWith(error -> {
            Effectful<A> result = recovery.apply(error);
            if (!(result instanceof VIOPath<?> VIOPath)) {
                throw new IllegalArgumentException("recovery must return VIOPath, got: " + result.getClass());
            }
            return ((VIOPath<A>) VIOPath).value;
        }));
    }

    @Override
    public VIOPath<A> guarantee(Runnable finalizer) {
        return new VIOPath<>(() -> {
            try {
                return value.unsafeRun();
            } finally {
                finalizer.run();
            }
        });
    }

    public VIOPath<Maybe<A>> asMaybe() {
        return new VIOPath<>(() -> runSafe().toMaybe());
    }

    public VIOPath<Try<A>> asTry() {
        return new VIOPath<>(this::runSafe);
    }

}

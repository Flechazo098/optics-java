package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Tuple2;

import com.flechazo.hkt.ThrowableSupplier;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.control.TryPath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.function.Function3;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LazyPath<A> implements Chainable<A> {
    private final Lazy<A> value;

    public LazyPath(Lazy<A> value) {
        this.value = value;
    }

    public static <A> LazyPath<A> now(A value) {
        return new LazyPath<>(Lazy.now(value));
    }

    public static <A> LazyPath<A> defer(Supplier<? extends A> supplier) {
        return new LazyPath<>(Lazy.defer(supplier::get));
    }

    public static <A> LazyPath<A> deferThrowable(ThrowableSupplier<? extends A> supplier) {
        return new LazyPath<>(Lazy.defer(supplier));
    }

    public A get() {
        try {
            return value.force();
        } catch (RuntimeException runtime) {
            throw runtime;
        } catch (Throwable error) {
            throw new RuntimeException(error);
        }
    }

    public A force() throws Throwable {
        return value.force();
    }

    public boolean isEvaluated() {
        return value.isEvaluated();
    }

    @Override
    public <B> LazyPath<B> map(Function<? super A, ? extends B> mapper) {
        return new LazyPath<>(value.map(mapper));
    }

    @Override
    public LazyPath<A> peek(Consumer<? super A> consumer) {
        return map(result -> {
            consumer.accept(result);
            return result;
        });
    }

    @Override
    public <B, C> LazyPath<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof LazyPath<?> lazyPath)) {
            throw new IllegalArgumentException("Cannot zipWith non-LazyPath: " + other.getClass());
        }
        LazyPath<B> typedOther = (LazyPath<B>) lazyPath;
        return new LazyPath<>(value.flatMap(left -> typedOther.value.map(right -> combiner.apply(left, right))));
    }

    @Override
    public <B, C, D> LazyPath<D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWith(second, Tuple2::new)
                .zipWith(third, (tuple, c) -> combiner.apply(tuple.first(), tuple.second(), c));
    }

    @Override
    public <B> LazyPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        return new LazyPath<>(value.flatMap(result -> {
            Chainable<B> mapped = mapper.apply(result);
            if (!(mapped instanceof LazyPath<?> lazyPath)) {
                throw new IllegalArgumentException("via mapper must return LazyPath, got: " + mapped.getClass());
            }
            return ((LazyPath<B>) lazyPath).value;
        }));
    }

    @Override
    public <B> LazyPath<B> then(Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    public VIOPath<A> toVIOPath() {
        return Pathway.vio(() -> {
            try {
                return force();
            } catch (Exception exception) {
                throw exception;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    public MaybePath<A> toMaybePath() {
        return Pathway.just(get());
    }

    public TryPath<A> toTryPath() {
        try {
            return new TryPath<>(Try.success(force()));
        } catch (Throwable throwable) {
            return new TryPath<>(Try.failure(throwable));
        }
    }
}

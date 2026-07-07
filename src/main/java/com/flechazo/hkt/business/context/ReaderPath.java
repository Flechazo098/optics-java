package com.flechazo.hkt.business.context;

import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.effect.IOPath;
import com.flechazo.hkt.function.Function3;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ReaderPath<R, A> implements Chainable<A> {
    private final Reader<R, A> value;

    public ReaderPath(Reader<R, A> value) {
        this.value = value;
    }

    public static <R, A> ReaderPath<R, A> pure(A value) {
        return new ReaderPath<>(Reader.constant(value));
    }

    public static <R> ReaderPath<R, R> ask() {
        return new ReaderPath<>(Reader.ask());
    }

    public static <R, A> ReaderPath<R, A> asks(Function<? super R, ? extends A> mapper) {
        return new ReaderPath<>(Reader.of(mapper));
    }

    public A run(R environment) {
        return value.run(environment);
    }

    public Reader<R, A> toReader() {
        return value;
    }

    @Override
    public <B> ReaderPath<R, B> map(Function<? super A, ? extends B> mapper) {
        return new ReaderPath<>(value.map(mapper));
    }

    @Override
    public ReaderPath<R, A> peek(Consumer<? super A> consumer) {
        return new ReaderPath<>(value.map(result -> {
            consumer.accept(result);
            return result;
        }));
    }

    @Override
    public <B, C> ReaderPath<R, C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof ReaderPath<?, ?> otherReader)) {
            throw new IllegalArgumentException("Cannot zipWith non-ReaderPath: " + other.getClass());
        }
        ReaderPath<R, B> typedOther = (ReaderPath<R, B>) otherReader;
        return new ReaderPath<>(environment -> combiner.apply(value.run(environment), typedOther.value.run(environment)));
    }

    @Override
    public <B, C, D> ReaderPath<R, D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWith(second, Combinable.Pair2::new)
                .zipWith(third, (pair, c) -> combiner.apply(pair.first(), pair.second(), c));
    }

    @Override
    public <B> ReaderPath<R, B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        return new ReaderPath<>(environment -> {
            Chainable<B> result = mapper.apply(value.run(environment));
            if (!(result instanceof ReaderPath<?, ?> readerPath)) {
                throw new IllegalArgumentException("via mapper must return ReaderPath, got: " + result.getClass());
            }
            return ((ReaderPath<R, B>) readerPath).run(environment);
        });
    }

    @Override
    public <B> ReaderPath<R, B> then(Supplier<? extends Chainable<B>> supplier) {
        return new ReaderPath<>(environment -> {
            value.run(environment);
            Chainable<B> result = supplier.get();
            if (!(result instanceof ReaderPath<?, ?> readerPath)) {
                throw new IllegalArgumentException("then supplier must return ReaderPath, got: " + result.getClass());
            }
            return ((ReaderPath<R, B>) readerPath).run(environment);
        });
    }

    public <R2> ReaderPath<R2, A> local(Function<? super R2, ? extends R> mapper) {
        return new ReaderPath<>(environment -> value.run(mapper.apply(environment)));
    }

    public IOPath<A> toIOPath(R environment) {
        return Pathway.ioPure(run(environment));
    }

    public MaybePath<A> toMaybePath(R environment) {
        return Pathway.just(run(environment));
    }
}

package com.flechazo.hkt.business.context;

import com.flechazo.hkt.Tuple2;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.business.control.EitherPath;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.effect.VIOPath;
import com.flechazo.hkt.function.Function3;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class WriterPath<W, A> implements Chainable<A> {
    private final Writer<W, A> value;
    private final Monoid<W> monoid;

    public WriterPath(Writer<W, A> value, Monoid<W> monoid) {
        this.value = value;
        this.monoid = monoid;
    }

    public static <W, A> WriterPath<W, A> pure(A value, Monoid<W> monoid) {
        return new WriterPath<>(Writer.value(monoid, value), monoid);
    }

    public static <W> WriterPath<W, Unit> tell(W log, Monoid<W> monoid) {
        return new WriterPath<>(Writer.tell(log), monoid);
    }

    public static <W, A> WriterPath<W, A> writer(A value, W log, Monoid<W> monoid) {
        return new WriterPath<>(Writer.of(log, value), monoid);
    }

    public Writer<W, A> run() {
        return value;
    }

    public A value() {
        return value.value();
    }

    public W written() {
        return value.written();
    }

    public Monoid<W> monoid() {
        return monoid;
    }

    @Override
    public <B> WriterPath<W, B> map(Function<? super A, ? extends B> mapper) {
        return new WriterPath<>(value.map(mapper), monoid);
    }

    @Override
    public WriterPath<W, A> peek(Consumer<? super A> consumer) {
        consumer.accept(value.value());
        return this;
    }

    @Override
    public <B, C> WriterPath<W, C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof WriterPath<?, ?> otherWriter)) {
            throw new IllegalArgumentException("Cannot zipWith non-WriterPath: " + other.getClass());
        }
        WriterPath<W, B> typedOther = (WriterPath<W, B>) otherWriter;
        return new WriterPath<>(
                Writer.of(
                        monoid.combine(value.written(), typedOther.value.written()),
                        combiner.apply(value.value(), typedOther.value.value())),
                monoid);
    }

    @Override
    public <B, C, D> WriterPath<W, D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWith(second, Tuple2::new)
                .zipWith(third, (tuple, c) -> combiner.apply(tuple.first(), tuple.second(), c));
    }

    @Override
    public <B> WriterPath<W, B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        Chainable<B> result = mapper.apply(value.value());
        if (!(result instanceof WriterPath<?, ?> writerPath)) {
            throw new IllegalArgumentException("via mapper must return WriterPath, got: " + result.getClass());
        }
        WriterPath<W, B> typedResult = (WriterPath<W, B>) writerPath;
        return new WriterPath<>(
                Writer.of(
                        monoid.combine(value.written(), typedResult.value.written()),
                        typedResult.value.value()),
                monoid);
    }

    @Override
    public <B> WriterPath<W, B> then(Supplier<? extends Chainable<B>> supplier) {
        Chainable<B> result = supplier.get();
        if (!(result instanceof WriterPath<?, ?> writerPath)) {
            throw new IllegalArgumentException("then supplier must return WriterPath, got: " + result.getClass());
        }
        WriterPath<W, B> typedResult = (WriterPath<W, B>) writerPath;
        return new WriterPath<>(
                Writer.of(
                        monoid.combine(value.written(), typedResult.value.written()),
                        typedResult.value.value()),
                monoid);
    }

    public WriterPath<W, A> censor(Function<? super W, ? extends W> mapper) {
        return new WriterPath<>(value.mapWritten(mapper), monoid);
    }

    public WriterPath<W, A> listen(W additionalLog) {
        return new WriterPath<>(Writer.of(monoid.combine(value.written(), additionalLog), value.value()), monoid);
    }

    public VIOPath<A> toVIOPath() {
        return Pathway.vioPure(value.value());
    }

    public MaybePath<A> toMaybePath() {
        return Pathway.just(value.value());
    }

    public <E> EitherPath<E, A> toEitherPath(E errorIfNoValue) {
        return Pathway.right(value.value());
    }
}

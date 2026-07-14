package com.flechazo.hkt.business.context;


import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.WriterCombinable;
import com.flechazo.hkt.business.control.EitherPath;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.effect.IOPath;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides fluent composition for computations that accumulate output.
 *
 * @param <W> the output type
 * @param <A> the result type
 */
public final class WriterPath<W, A> implements Chainable<A>, WriterCombinable<W, A> {
    private final Writer<W, A> value;
    private final Monoid<W> monoid;

    /**
     * Creates a path over a writer and its output monoid.
     *
     * @param value the writer value
     * @param monoid the operation used to combine output
     */
    public WriterPath(Writer<W, A> value, Monoid<W> monoid) {
        this.value = value;
        this.monoid = monoid;
    }

    /**
     * Creates a path with empty output and a value.
     *
     * @param <W> the output type
     * @param <A> the result type
     * @param value the result value
     * @param monoid the output monoid
     * @return a pure writer path
     */
    public static <W, A> WriterPath<W, A> pure(A value, Monoid<W> monoid) {
        return new WriterPath<>(Writer.value(monoid, value), monoid);
    }

    /**
     * Creates a path that emits output and returns {@link Unit}.
     *
     * @param <W> the output type
     * @param log the output to emit
     * @param monoid the output monoid
     * @return an output-only writer path
     */
    public static <W> WriterPath<W, Unit> tell(W log, Monoid<W> monoid) {
        return new WriterPath<>(Writer.tell(log), monoid);
    }

    /**
     * Creates a path from a result and initial output.
     *
     * @param <W> the output type
     * @param <A> the result type
     * @param value the result value
     * @param log the initial output
     * @param monoid the output monoid
     * @return a writer path containing the supplied values
     */
    public static <W, A> WriterPath<W, A> writer(A value, W log, Monoid<W> monoid) {
        return new WriterPath<>(Writer.of(log, value), monoid);
    }

    /**
     * Returns the underlying writer value.
     *
     * @return the writer represented by this path
     */
    public Writer<W, A> run() {
        return value;
    }

    /**
     * Returns the computed value.
     *
     * @return the computed value
     */
    public A value() {
        return value.value();
    }

    /**
     * Returns the accumulated output.
     *
     * @return the accumulated output
     */
    public W written() {
        return value.written();
    }

    /**
     * Returns the monoid used to combine output.
     *
     * @return the output monoid
     */
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

    /**
     * Combines two writer results and accumulates their output in order.
     *
     * @param <B> the other result type
     * @param <C> the combined result type
     * @param other the writer path to combine with this path
     * @param combiner the function combining both results
     * @return the combined writer path
     * @throws IllegalArgumentException if {@code other} is not a writer path
     */
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

    /**
     * Transforms the accumulated output.
     *
     * @param mapper the output transformation
     * @return a path with transformed output
     */
    public WriterPath<W, A> censor(Function<? super W, ? extends W> mapper) {
        return new WriterPath<>(value.mapWritten(mapper), monoid);
    }

    /**
     * Appends output to this path.
     *
     * @param additionalLog the output to append
     * @return a path with combined output
     */
    public WriterPath<W, A> listen(W additionalLog) {
        return new WriterPath<>(Writer.of(monoid.combine(value.written(), additionalLog), value.value()), monoid);
    }

    /**
     * Lifts the computed value into an IO path and discards accumulated output.
     *
     * @return an IO path containing the computed value
     */
    public IOPath<A> toIOPath() {
        return Pathway.ioPure(value.value());
    }

    /**
     * Wraps the computed value in a defined maybe path and discards accumulated output.
     *
     * @return a defined maybe path containing the computed value
     */
    public MaybePath<A> toMaybePath() {
        return Pathway.just(value.value());
    }

    /**
     * Wraps the computed value in a right either path and discards accumulated output.
     *
     * @param <E> the error type
     * @param errorIfNoValue the error type witness for the resulting path
     * @return a right either path containing the computed value
     */
    public <E> EitherPath<E, A> toEitherPath(E errorIfNoValue) {
        return Pathway.right(value.value());
    }
}

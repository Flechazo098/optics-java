package com.flechazo.hkt.business.context;


import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.ReaderCombinable;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.effect.IOPath;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides fluent composition for environment-dependent computations.
 *
 * @param <R> the environment type
 * @param <A> the result type
 */
public final class ReaderPath<R, A> implements Chainable<A>, ReaderCombinable<R, A> {
    private final Reader<R, A> value;

    /**
     * Creates a path over a reader computation.
     *
     * @param value the reader computation
     */
    public ReaderPath(Reader<R, A> value) {
        this.value = value;
    }

    /**
     * Creates a path that ignores its environment and returns a value.
     *
     * @param <R> the environment type
     * @param <A> the result type
     * @param value the result value
     * @return a constant reader path
     */
    public static <R, A> ReaderPath<R, A> pure(A value) {
        return new ReaderPath<>(Reader.constant(value));
    }

    /**
     * Creates a path that returns its environment.
     *
     * @param <R> the environment type
     * @return an environment-reading path
     */
    public static <R> ReaderPath<R, R> ask() {
        return new ReaderPath<>(Reader.ask());
    }

    /**
     * Creates a path that derives a result from its environment.
     *
     * @param <R> the environment type
     * @param <A> the result type
     * @param mapper the environment projection
     * @return a reader path backed by {@code mapper}
     */
    public static <R, A> ReaderPath<R, A> asks(Function<? super R, ? extends A> mapper) {
        return new ReaderPath<>(Reader.of(mapper));
    }

    /**
     * Runs this path with an environment.
     *
     * @param environment the environment supplied to the computation
     * @return the computed result
     */
    public A run(R environment) {
        return value.run(environment);
    }

    /**
     * Returns the underlying reader computation.
     *
     * @return the reader represented by this path
     */
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

    /**
     * Combines the results of two reader paths using the same environment.
     *
     * @param <B> the other result type
     * @param <C> the combined result type
     * @param other the reader path to combine with this path
     * @param combiner the function combining both results
     * @return the combined reader path
     * @throws IllegalArgumentException if {@code other} is not a reader path
     */
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

    /**
     * Changes the required environment by transforming a new environment.
     *
     * @param <R2> the new environment type
     * @param mapper the function converting a new environment to the current environment
     * @return a reader path accepting the new environment type
     */
    public <R2> ReaderPath<R2, A> local(Function<? super R2, ? extends R> mapper) {
        return new ReaderPath<>(environment -> value.run(mapper.apply(environment)));
    }

    /**
     * Evaluates this path with an environment and lifts the result into an IO path.
     *
     * @param environment the environment supplied to the computation
     * @return an IO path producing the reader result
     */
    public IOPath<A> toIOPath(R environment) {
        return Pathway.ioPure(run(environment));
    }

    /**
     * Evaluates this path with an environment and wraps the result in a defined maybe path.
     *
     * @param environment the environment supplied to the computation
     * @return a defined maybe path containing the reader result
     */
    public MaybePath<A> toMaybePath(R environment) {
        return Pathway.just(run(environment));
    }
}

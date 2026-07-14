package com.flechazo.hkt.business.context;


import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.StateCombinable;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.data.StateResult;
import com.flechazo.hkt.business.effect.IOPath;

import java.util.function.*;

/**
 * Provides fluent composition for stateful computations.
 *
 * @param <S> the state type
 * @param <A> the result type
 */
public final class WithStatePath<S, A> implements Chainable<A>, StateCombinable<S, A> {
    private final State<S, A> value;

    /**
     * Creates a path over a stateful computation.
     *
     * @param value the stateful computation
     */
    public WithStatePath(State<S, A> value) {
        this.value = value;
    }

    /**
     * Creates a path that preserves state and returns a value.
     *
     * @param <S> the state type
     * @param <A> the result type
     * @param value the result value
     * @return a pure state path
     */
    public static <S, A> WithStatePath<S, A> pure(A value) {
        return new WithStatePath<>(State.pure(value));
    }

    /**
     * Creates a path that returns the current state.
     *
     * @param <S> the state type
     * @return a state-reading path
     */
    public static <S> WithStatePath<S, S> get() {
        return new WithStatePath<>(State.get());
    }

    /**
     * Creates a path that replaces the current state.
     *
     * @param <S> the state type
     * @param newState the replacement state
     * @return a path producing {@link Unit}
     */
    public static <S> WithStatePath<S, Unit> set(S newState) {
        return new WithStatePath<>(State.set(newState));
    }

    /**
     * Creates a path that transforms the current state.
     *
     * @param <S> the state type
     * @param mapper the state transformation
     * @return a path producing {@link Unit}
     */
    public static <S> WithStatePath<S, Unit> modify(UnaryOperator<S> mapper) {
        return new WithStatePath<>(State.modify(mapper));
    }

    /**
     * Creates a path that derives a result from the current state without changing it.
     *
     * @param <S> the state type
     * @param <A> the result type
     * @param mapper the state projection
     * @return a state-inspecting path
     */
    public static <S, A> WithStatePath<S, A> inspect(Function<? super S, ? extends A> mapper) {
        return new WithStatePath<>(state -> new StateResult<>(state, mapper.apply(state)));
    }

    /**
     * Runs this path with an initial state.
     *
     * @param initialState the initial state
     * @return the final state and computed result
     */
    public StateResult<S, A> run(S initialState) {
        return value.run(initialState);
    }

    /**
     * Runs this path and returns only its result.
     *
     * @param initialState the initial state
     * @return the computed result
     */
    public A evalState(S initialState) {
        return value.evalState(initialState);
    }

    /**
     * Runs this path and returns only its final state.
     *
     * @param initialState the initial state
     * @return the final state
     */
    public S execState(S initialState) {
        return value.execState(initialState);
    }

    /**
     * Returns the underlying stateful computation.
     *
     * @return the stateful computation represented by this path
     */
    public State<S, A> toState() {
        return value;
    }

    @Override
    public <B> WithStatePath<S, B> map(Function<? super A, ? extends B> mapper) {
        return new WithStatePath<>(value.map(mapper));
    }

    @Override
    public WithStatePath<S, A> peek(Consumer<? super A> consumer) {
        return new WithStatePath<>(value.map(result -> {
            consumer.accept(result);
            return result;
        }));
    }

    /**
     * Runs two state paths in order and combines their results.
     *
     * @param <B> the other result type
     * @param <C> the combined result type
     * @param other the state path to run after this path
     * @param combiner the function combining both results
     * @return the combined state path
     * @throws IllegalArgumentException if {@code other} is not a state path
     */
    @Override
    public <B, C> WithStatePath<S, C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof WithStatePath<?, ?> otherState)) {
            throw new IllegalArgumentException("Cannot zipWith non-WithStatePath: " + other.getClass());
        }
        WithStatePath<S, B> typedOther = (WithStatePath<S, B>) otherState;
        return new WithStatePath<>(state -> {
            StateResult<S, A> left = value.run(state);
            StateResult<S, B> right = typedOther.value.run(left.state());
            return new StateResult<>(right.state(), combiner.apply(left.value(), right.value()));
        });
    }

    @Override
    public <B> WithStatePath<S, B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        return new WithStatePath<>(state -> {
            StateResult<S, A> result = value.run(state);
            Chainable<B> mapped = mapper.apply(result.value());
            if (!(mapped instanceof WithStatePath<?, ?> statePath)) {
                throw new IllegalArgumentException("via mapper must return WithStatePath, got: " + mapped.getClass());
            }
            return ((WithStatePath<S, B>) statePath).value.run(result.state());
        });
    }

    @Override
    public <B> WithStatePath<S, B> then(Supplier<? extends Chainable<B>> supplier) {
        return new WithStatePath<>(state -> {
            StateResult<S, A> result = value.run(state);
            Chainable<B> next = supplier.get();
            if (!(next instanceof WithStatePath<?, ?> statePath)) {
                throw new IllegalArgumentException("then supplier must return WithStatePath, got: " + next.getClass());
            }
            return ((WithStatePath<S, B>) statePath).value.run(result.state());
        });
    }

    /**
     * Runs this path and lifts its result into an IO path.
     *
     * @param initialState the initial state
     * @return an IO path producing the stateful result
     */
    public IOPath<A> toIOPath(S initialState) {
        return Pathway.ioPure(evalState(initialState));
    }

    /**
     * Runs this path and wraps its result in a defined maybe path.
     *
     * @param initialState the initial state
     * @return a defined maybe path containing the stateful result
     */
    public MaybePath<A> toMaybePath(S initialState) {
        return Pathway.just(evalState(initialState));
    }
}

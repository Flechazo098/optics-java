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

public final class WithStatePath<S, A> implements Chainable<A>, StateCombinable<S, A> {
    private final State<S, A> value;

    public WithStatePath(State<S, A> value) {
        this.value = value;
    }

    public static <S, A> WithStatePath<S, A> pure(A value) {
        return new WithStatePath<>(State.pure(value));
    }

    public static <S> WithStatePath<S, S> get() {
        return new WithStatePath<>(State.get());
    }

    public static <S> WithStatePath<S, Unit> set(S newState) {
        return new WithStatePath<>(State.set(newState));
    }

    public static <S> WithStatePath<S, Unit> modify(UnaryOperator<S> mapper) {
        return new WithStatePath<>(State.modify(mapper));
    }

    public static <S, A> WithStatePath<S, A> inspect(Function<? super S, ? extends A> mapper) {
        return new WithStatePath<>(state -> new StateResult<>(state, mapper.apply(state)));
    }

    public StateResult<S, A> run(S initialState) {
        return value.run(initialState);
    }

    public A evalState(S initialState) {
        return value.evalState(initialState);
    }

    public S execState(S initialState) {
        return value.execState(initialState);
    }

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

    public IOPath<A> toIOPath(S initialState) {
        return Pathway.ioPure(evalState(initialState));
    }

    public MaybePath<A> toMaybePath(S initialState) {
        return Pathway.just(evalState(initialState));
    }
}

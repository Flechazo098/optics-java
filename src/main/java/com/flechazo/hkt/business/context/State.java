package com.flechazo.hkt.business.context;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.data.StateTuple;

import java.util.function.Function;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface State<S, A> {
    static <S, A> State<S, A> of(Function<? super S, StateTuple<S, A>> run) {
        return run::apply;
    }

    static <S, A> State<S, A> pure(A value) {
        return state -> new StateTuple<>(state, value);
    }

    static <S> State<S, S> get() {
        return state -> new StateTuple<>(state, state);
    }

    static <S> State<S, Unit> set(S state) {
        return ignored -> new StateTuple<>(state, Unit.INSTANCE);
    }

    static <S> State<S, Unit> modify(UnaryOperator<S> mapper) {
        return state -> new StateTuple<>(mapper.apply(state), Unit.INSTANCE);
    }

    StateTuple<S, A> run(S state);

    default A evalState(S state) {
        return run(state).value();
    }

    default S execState(S state) {
        return run(state).state();
    }

    default <B> State<S, B> map(Function<? super A, ? extends B> mapper) {
        return state -> {
            StateTuple<S, A> result = run(state);
            return new StateTuple<>(result.state(), mapper.apply(result.value()));
        };
    }

    default <B> State<S, B> flatMap(Function<? super A, ? extends State<S, B>> mapper) {
        return state -> {
            StateTuple<S, A> result = run(state);
            return mapper.apply(result.value()).run(result.state());
        };
    }
}

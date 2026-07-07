package com.flechazo.hkt.business.context;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monad;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.data.StateTuple;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface State<S, A> extends App<State.Mu<S>, A> {
    final class Mu<S> implements K1 {
        private Mu() {
        }
    }

    final class InstanceMu implements Applicative.Mu {
        private InstanceMu() {
        }
    }

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

    static <S, A> State<S, A> unbox(App<Mu<S>, A> value) {
        return (State<S, A>) value;
    }

    static <S> Applicative<State.Mu<S>, InstanceMu> applicative() {
        return Instance.instance();
    }

    static <S> Monad<State.Mu<S>, InstanceMu> monad() {
        return Instance.instance();
    }

    static <S> Selective<State.Mu<S>, InstanceMu> selective() {
        return Instance.instance();
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

    final class Instance<S> implements Monad<State.Mu<S>, InstanceMu>, Selective<State.Mu<S>, InstanceMu> {
        private static final Instance<?> INSTANCE = new Instance<>();

        private Instance() {
        }

        @SuppressWarnings("unchecked")
        static <S> Instance<S> instance() {
            return (Instance<S>) INSTANCE;
        }

        @Override
        public <A> App<State.Mu<S>, A> of(A value) {
            return State.pure(value);
        }

        @Override
        public <A, B> App<State.Mu<S>, B> flatMap(
                Function<? super A, ? extends App<State.Mu<S>, B>> f,
                App<State.Mu<S>, A> fa) {
            return State.unbox(fa).flatMap(value -> State.unbox(f.apply(value)));
        }

        @Override
        public <A, B> App<State.Mu<S>, B> select(
                App<State.Mu<S>, Either<A, B>> value,
                App<State.Mu<S>, ? extends Function<A, B>> function) {
            State<S, B> result = state -> {
                StateTuple<S, Either<A, B>> selected = State.unbox(value).run(state);
                Either<A, B> either = selected.value();
                if (either.isRight()) {
                    return new StateTuple<>(selected.state(), either.right());
                }
                StateTuple<S, ? extends Function<A, B>> fn = State.unbox(function).run(selected.state());
                return new StateTuple<>(fn.state(), fn.value().apply(either.left()));
            };
            return result;
        }

        @Override
        public <A> App<State.Mu<S>, A> ifS(
                App<State.Mu<S>, Boolean> condition,
                Supplier<? extends App<State.Mu<S>, A>> thenValue,
                Supplier<? extends App<State.Mu<S>, A>> elseValue) {
            State<S, A> result = state -> {
                StateTuple<S, Boolean> test = State.unbox(condition).run(state);
                Supplier<? extends App<State.Mu<S>, A>> branch =
                        Boolean.TRUE.equals(test.value()) ? thenValue : elseValue;
                return State.unbox(branch.get()).run(test.state());
            };
            return result;
        }
    }
}

package com.flechazo.hkt.business.context;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.data.StateResult;
import com.flechazo.hkt.util.validation.Validation;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Represents a computation that reads and produces state together with a result.
 *
 * @param <S> the state type
 * @param <A> the result type
 */
@FunctionalInterface
public interface State<S, A> extends App<State.Mu<S>, A> {
    final class Mu<S> implements K1 {
        private Mu() {
        }
    }

    /**
     * Creates a stateful computation from a state transition function.
     *
     * @param <S> the state type
     * @param <A> the result type
     * @param run the state transition
     * @return a stateful computation backed by {@code run}
     */
    static <S, A> State<S, A> of(Function<? super S, StateResult<S, A>> run) {
        return run::apply;
    }

    /**
     * Creates a computation that preserves state and returns a value.
     *
     * @param <S> the state type
     * @param <A> the result type
     * @param value the result value
     * @return a pure stateful computation
     */
    static <S, A> State<S, A> pure(A value) {
        return state -> new StateResult<>(state, value);
    }

    /**
     * Creates a computation that returns the current state.
     *
     * @param <S> the state type
     * @return a state-reading computation
     */
    static <S> State<S, S> get() {
        return state -> new StateResult<>(state, state);
    }

    /**
     * Creates a computation that replaces the current state.
     *
     * @param <S> the state type
     * @param state the replacement state
     * @return a computation producing {@link Unit}
     */
    static <S> State<S, Unit> set(S state) {
        return ignored -> new StateResult<>(state, Unit.INSTANCE);
    }

    /**
     * Creates a computation that transforms the current state.
     *
     * @param <S> the state type
     * @param mapper the state transformation
     * @return a computation producing {@link Unit}
     */
    static <S> State<S, Unit> modify(UnaryOperator<S> mapper) {
        return state -> new StateResult<>(mapper.apply(state), Unit.INSTANCE);
    }

    /**
     * Narrows an encoded stateful value.
     *
     * @param <S> the state type
     * @param <A> the result type
     * @param value the encoded stateful computation
     * @return the concrete stateful computation
     * @throws com.flechazo.hkt.exception.KindUnwrapException if {@code value} is not a stateful computation
     */
    static <S, A> State<S, A> unbox(App<Mu<S>, A> value) {
        return (State<S, A>) Validation.kind().narrowWithTypeCheck(value, State.class);
    }

    /**
     * Returns the state applicative instance.
     *
     * @param <S> the state type
     * @return the state applicative
     */
    static <S> Applicative<State.Mu<S>, Instance.Mu> applicative() {
        return Instance.instance();
    }

    /**
     * Returns the state monad instance.
     *
     * @param <S> the state type
     * @return the state monad
     */
    static <S> Monad<State.Mu<S>, Instance.Mu> monad() {
        return Instance.instance();
    }

    /**
     * Returns the state selective instance.
     *
     * @param <S> the state type
     * @return the state selective
     */
    static <S> Selective<State.Mu<S>, Instance.Mu> selective() {
        return Instance.instance();
    }

    /**
     * Runs this computation with an initial state.
     *
     * @param state the initial state
     * @return the final state and computed result
     */
    StateResult<S, A> run(S state);

    /**
     * Runs this computation and returns only its result.
     *
     * @param state the initial state
     * @return the computed result
     */
    default A evalState(S state) {
        return run(state).value();
    }

    /**
     * Runs this computation and returns only its final state.
     *
     * @param state the initial state
     * @return the final state
     */
    default S execState(S state) {
        return run(state).state();
    }

    /**
     * Transforms the result while preserving the state transition.
     *
     * @param <B> the transformed result type
     * @param mapper the result transformation
     * @return the transformed stateful computation
     */
    default <B> State<S, B> map(Function<? super A, ? extends B> mapper) {
        return state -> {
            StateResult<S, A> result = run(state);
            return new StateResult<>(result.state(), mapper.apply(result.value()));
        };
    }

    /**
     * Sequences a stateful computation selected from the current result.
     *
     * @param <B> the next result type
     * @param mapper the function selecting the next computation
     * @return the sequenced stateful computation
     */
    default <B> State<S, B> flatMap(Function<? super A, ? extends State<S, B>> mapper) {
        return state -> {
            StateResult<S, A> result = run(state);
            return mapper.apply(result.value()).run(result.state());
        };
    }

    /**
     * Provides the state monad and selective operations.
     *
     * @param <S> the state type
     */
    final class Instance<S> implements Monad<State.Mu<S>, Instance.Mu>, Selective<State.Mu<S>, Instance.Mu> {
        private static final Instance<?> INSTANCE = new Instance<>();

        public static final class Mu implements Applicative.Mu {
            private Mu() {
            }
        }

        private Instance() {
        }

        @SuppressWarnings("unchecked")
        static <S> Instance<S> instance() {
            return (Instance<S>) INSTANCE;
        }

        /**
         * Creates a stateful computation that preserves state and returns a value.
         *
         * @param <A> the value type
         * @param value the result value
         * @return the pure stateful computation in encoded form
         */
        @Override
        public <A> App<State.Mu<S>, A> of(A value) {
            return State.pure(value);
        }

        /**
         * Sequences an encoded stateful computation selected from the current result.
         *
         * @param <A> the source result type
         * @param <B> the next result type
         * @param f the function selecting the next computation
         * @param fa the source computation
         * @return the sequenced computation in encoded form
         */
        @Override
        public <A, B> App<State.Mu<S>, B> flatMap(
                Function<? super A, ? extends App<State.Mu<S>, B>> f,
                App<State.Mu<S>, A> fa) {
            return State.unbox(fa).flatMap(value -> State.unbox(f.apply(value)));
        }

        /**
         * Resolves an encoded either value and threads state through the required function branch.
         *
         * @param <A> the function argument type
         * @param <B> the result type
         * @param value the stateful branch value
         * @param function the stateful function used for a left branch
         * @return the selected result in encoded state form
         */
        @Override
        public <A, B> App<State.Mu<S>, B> select(
                App<State.Mu<S>, Either<A, B>> value,
                App<State.Mu<S>, ? extends Function<A, B>> function) {
            State<S, B> result = state -> {
                StateResult<S, Either<A, B>> selected = State.unbox(value).run(state);
                Either<A, B> either = selected.value();
                if (either.isRight()) {
                    return new StateResult<>(selected.state(), either.right());
                }
                StateResult<S, ? extends Function<A, B>> fn = State.unbox(function).run(selected.state());
                return new StateResult<>(fn.state(), fn.value().apply(either.left()));
            };
            return result;
        }

        /**
         * Evaluates one deferred stateful branch and threads state through the condition and branch.
         *
         * @param <A> the result type
         * @param condition the stateful condition
         * @param thenValue the deferred computation used for a true condition
         * @param elseValue the deferred computation used for a false condition
         * @return the selected branch in encoded state form
         */
        @Override
        public <A> App<State.Mu<S>, A> ifS(
                App<State.Mu<S>, Boolean> condition,
                Supplier<? extends App<State.Mu<S>, A>> thenValue,
                Supplier<? extends App<State.Mu<S>, A>> elseValue) {
            State<S, A> result = state -> {
                StateResult<S, Boolean> test = State.unbox(condition).run(state);
                Supplier<? extends App<State.Mu<S>, A>> branch =
                        test.value() ? thenValue : elseValue;
                return State.unbox(branch.get()).run(test.state());
            };
            return result;
        }
    }
}

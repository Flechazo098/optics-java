package com.flechazo.hkt.business.context;

import com.flechazo.hkt.*;
import com.flechazo.hkt.util.validation.Validation;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a computation that derives a result from a shared environment.
 *
 * @param <R> the environment type
 * @param <A> the result type
 */
@FunctionalInterface
public interface Reader<R, A> extends App<Reader.Mu<R>, A> {
    final class Mu<R> implements K1 {
        private Mu() {
        }
    }

    /**
     * Creates a reader from an environment function.
     *
     * @param <R> the environment type
     * @param <A> the result type
     * @param run the environment function
     * @return a reader backed by {@code run}
     */
    static <R, A> Reader<R, A> of(Function<? super R, ? extends A> run) {
        return run::apply;
    }

    /**
     * Creates a reader that ignores its environment and returns a value.
     *
     * @param <R> the environment type
     * @param <A> the result type
     * @param value the constant result
     * @return a constant reader
     */
    static <R, A> Reader<R, A> constant(A value) {
        return ignored -> value;
    }

    /**
     * Creates a reader that returns its environment.
     *
     * @param <R> the environment type
     * @return an environment reader
     */
    static <R> Reader<R, R> ask() {
        return environment -> environment;
    }

    /**
     * Narrows an encoded reader value.
     *
     * @param <R> the environment type
     * @param <A> the result type
     * @param value the encoded reader
     * @return the concrete reader
     * @throws com.flechazo.hkt.exception.KindUnwrapException if {@code value} is not a reader
     */
    static <R, A> Reader<R, A> unbox(App<Mu<R>, A> value) {
        return (Reader<R, A>) Validation.kind().narrowWithTypeCheck(value, Reader.class);
    }

    /**
     * Returns the reader applicative instance.
     *
     * @param <R> the environment type
     * @return the reader applicative
     */
    static <R> Applicative<Reader.Mu<R>, Instance.Mu> applicative() {
        return Instance.instance();
    }

    /**
     * Returns the reader monad instance.
     *
     * @param <R> the environment type
     * @return the reader monad
     */
    static <R> Monad<Reader.Mu<R>, Instance.Mu> monad() {
        return Instance.instance();
    }

    /**
     * Returns the reader selective instance.
     *
     * @param <R> the environment type
     * @return the reader selective
     */
    static <R> Selective<Reader.Mu<R>, Instance.Mu> selective() {
        return Instance.instance();
    }

    /**
     * Runs this reader with an environment.
     *
     * @param environment the environment supplied to the computation
     * @return the computed result
     */
    A run(R environment);

    /**
     * Transforms the result while preserving environment access.
     *
     * @param <B> the transformed result type
     * @param mapper the result transformation
     * @return the transformed reader
     */
    default <B> Reader<R, B> map(Function<? super A, ? extends B> mapper) {
        return environment -> mapper.apply(run(environment));
    }

    /**
     * Sequences a reader selected from the current result using the same environment.
     *
     * @param <B> the next result type
     * @param mapper the function selecting the next reader
     * @return the sequenced reader
     */
    default <B> Reader<R, B> flatMap(Function<? super A, ? extends Reader<R, B>> mapper) {
        return environment -> mapper.apply(run(environment)).run(environment);
    }

    /**
     * Replaces the result with {@link Unit} while preserving environment-dependent evaluation.
     *
     * @return a reader producing {@link Unit}
     */
    default Reader<R, Unit> asUnit() {
        return map(ignored -> Unit.INSTANCE);
    }

    /**
     * Provides the reader monad and selective operations.
     *
     * @param <R> the environment type
     */
    final class Instance<R> implements Monad<Reader.Mu<R>, Instance.Mu>, Selective<Reader.Mu<R>, Instance.Mu> {
        private static final Instance<?> INSTANCE = new Instance<>();

        public static final class Mu implements Applicative.Mu {
            private Mu() {
            }
        }

        private Instance() {
        }

        @SuppressWarnings("unchecked")
        static <R> Instance<R> instance() {
            return (Instance<R>) INSTANCE;
        }

        /**
         * Creates a reader that returns a value for every environment.
         *
         * @param <A> the value type
         * @param value the value returned by the reader
         * @return the constant reader in encoded form
         */
        @Override
        public <A> App<Reader.Mu<R>, A> of(A value) {
            return Reader.constant(value);
        }

        /**
         * Sequences an encoded reader selected from the current result using the same environment.
         *
         * @param <A> the source result type
         * @param <B> the next result type
         * @param f the function selecting the next reader
         * @param fa the source reader
         * @return the sequenced reader in encoded form
         */
        @Override
        public <A, B> App<Reader.Mu<R>, B> flatMap(
                Function<? super A, ? extends App<Reader.Mu<R>, B>> f,
                App<Reader.Mu<R>, A> fa) {
            return Reader.unbox(fa).flatMap(value -> Reader.unbox(f.apply(value)));
        }

        /**
         * Resolves an encoded either value, evaluating the encoded function only for a left result.
         *
         * @param <A> the function argument type
         * @param <B> the result type
         * @param value the reader producing the branch value
         * @param function the reader producing the function for a left branch
         * @return the selected result in encoded reader form
         */
        @Override
        public <A, B> App<Reader.Mu<R>, B> select(
                App<Reader.Mu<R>, Either<A, B>> value,
                App<Reader.Mu<R>, ? extends Function<A, B>> function) {
            Reader<R, B> result = environment -> {
                Either<A, B> either = Reader.unbox(value).run(environment);
                return either.isRight()
                        ? either.right()
                        : Reader.unbox(function).run(environment).apply(either.left());
            };
            return result;
        }

        /**
         * Evaluates one deferred reader branch according to an environment-dependent condition.
         *
         * @param <A> the result type
         * @param condition the reader producing the condition
         * @param thenValue the deferred reader used for a true condition
         * @param elseValue the deferred reader used for a false condition
         * @return the selected branch in encoded reader form
         */
        @Override
        public <A> App<Reader.Mu<R>, A> ifS(
                App<Reader.Mu<R>, Boolean> condition,
                Supplier<? extends App<Reader.Mu<R>, A>> thenValue,
                Supplier<? extends App<Reader.Mu<R>, A>> elseValue) {
            Reader<R, A> result = environment -> {
                Supplier<? extends App<Reader.Mu<R>, A>> branch =
                        Boolean.TRUE.equals(Reader.unbox(condition).run(environment)) ? thenValue : elseValue;
                return Reader.unbox(branch.get()).run(environment);
            };
            return result;
        }
    }
}

package com.flechazo.hkt.business.context;

import com.flechazo.hkt.*;
import com.flechazo.hkt.util.validation.Validation;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Contains accumulated output and a computed value.
 *
 * @param <W> the output type
 * @param <A> the value type
 * @param written the accumulated output
 * @param value the computed value
 */
public record Writer<W, A>(W written, A value) implements App<Writer.Mu<W>, A> {
    public static final class Mu<W> implements K1 {
        private Mu() {
        }
    }

    /**
     * Creates a writer with empty output and a value.
     *
     * @param <W> the output type
     * @param <A> the value type
     * @param monoid the output monoid providing the empty value
     * @param value the computed value
     * @return a writer with empty output
     */
    public static <W, A> Writer<W, A> value(Monoid<W> monoid, A value) {
        return new Writer<>(monoid.empty(), value);
    }

    /**
     * Creates a writer from output and a value.
     *
     * @param <W> the output type
     * @param <A> the value type
     * @param written the output
     * @param value the computed value
     * @return a writer containing both values
     */
    public static <W, A> Writer<W, A> of(W written, A value) {
        return new Writer<>(written, value);
    }

    /**
     * Creates a writer that emits output and returns {@link Unit}.
     *
     * @param <W> the output type
     * @param written the output to emit
     * @return an output-only writer
     */
    public static <W> Writer<W, Unit> tell(W written) {
        return new Writer<>(written, Unit.INSTANCE);
    }

    /**
     * Narrows an encoded writer value.
     *
     * @param <W> the output type
     * @param <A> the value type
     * @param value the encoded writer
     * @return the concrete writer
     * @throws com.flechazo.hkt.exception.KindUnwrapException if {@code value} is not a writer
     */
    public static <W, A> Writer<W, A> unbox(App<Mu<W>, A> value) {
        return (Writer<W, A>) Validation.kind().narrowWithTypeCheck(value, Writer.class);
    }

    /**
     * Returns a writer applicative using an output monoid.
     *
     * @param <W> the output type
     * @param monoid the operation used to combine output
     * @return the writer applicative
     */
    public static <W> Applicative<Writer.Mu<W>, Instance.Mu> applicative(Monoid<W> monoid) {
        return new Instance<>(monoid);
    }

    /**
     * Returns a writer monad using an output monoid.
     *
     * @param <W> the output type
     * @param monoid the operation used to combine output
     * @return the writer monad
     */
    public static <W> Monad<Writer.Mu<W>, Instance.Mu> monad(Monoid<W> monoid) {
        return new Instance<>(monoid);
    }

    /**
     * Returns a writer selective using an output monoid.
     *
     * @param <W> the output type
     * @param monoid the operation used to combine output
     * @return the writer selective
     */
    public static <W> Selective<Writer.Mu<W>, Instance.Mu> selective(Monoid<W> monoid) {
        return new Instance<>(monoid);
    }

    /**
     * Returns the computed value.
     *
     * @return the computed value
     */
    public A run() {
        return value;
    }

    /**
     * Returns the accumulated output.
     *
     * @return the accumulated output
     */
    public W exec() {
        return written;
    }

    /**
     * Transforms the computed value while preserving output.
     *
     * @param <B> the transformed value type
     * @param mapper the value transformation
     * @return the transformed writer
     */
    public <B> Writer<W, B> map(Function<? super A, ? extends B> mapper) {
        return new Writer<>(written, mapper.apply(value));
    }

    /**
     * Transforms the accumulated output while preserving the value.
     *
     * @param <W2> the transformed output type
     * @param mapper the output transformation
     * @return the transformed writer
     */
    public <W2> Writer<W2, A> mapWritten(Function<? super W, ? extends W2> mapper) {
        return new Writer<>(mapper.apply(written), value);
    }

    /**
     * Sequences a writer selected from the current value and combines their output.
     *
     * @param <B> the next value type
     * @param monoid the operation used to combine output
     * @param mapper the function selecting the next writer
     * @return the sequenced writer
     */
    public <B> Writer<W, B> flatMap(Monoid<W> monoid, Function<? super A, Writer<W, B>> mapper) {
        Writer<W, B> next = mapper.apply(value);
        return new Writer<>(monoid.combine(written, next.written()), next.value());
    }

    /**
     * Provides writer monad and selective operations for an output monoid.
     *
     * @param <W> the output type
     */
    public static final class Instance<W>
            implements Monad<Writer.Mu<W>, Instance.Mu>, Selective<Writer.Mu<W>, Instance.Mu> {
        private final Monoid<W> monoid;

        private Instance(Monoid<W> monoid) {
            this.monoid = monoid;
        }

        public static final class Mu implements Applicative.Mu {
            private Mu() {
            }
        }

        /**
         * Creates a writer with empty output and a value.
         *
         * @param <A> the value type
         * @param value the result value
         * @return the writer in encoded form
         */
        @Override
        public <A> App<Writer.Mu<W>, A> of(A value) {
            return Writer.value(monoid, value);
        }

        /**
         * Sequences an encoded writer selected from the current value and combines their output.
         *
         * @param <A> the source value type
         * @param <B> the next value type
         * @param f the function selecting the next writer
         * @param fa the source writer
         * @return the sequenced writer in encoded form
         */
        @Override
        public <A, B> App<Writer.Mu<W>, B> flatMap(
                Function<? super A, ? extends App<Writer.Mu<W>, B>> f,
                App<Writer.Mu<W>, A> fa) {
            return Writer.unbox(fa).flatMap(monoid, value -> Writer.unbox(f.apply(value)));
        }

        /**
         * Resolves an encoded either value and combines output from an evaluated function branch.
         *
         * @param <A> the function argument type
         * @param <B> the result type
         * @param value the writer containing the branch value
         * @param function the writer containing the function for a left branch
         * @return the selected result and accumulated output in encoded writer form
         */
        @Override
        public <A, B> App<Writer.Mu<W>, B> select(
                App<Writer.Mu<W>, Either<A, B>> value,
                App<Writer.Mu<W>, ? extends Function<A, B>> function) {
            Writer<W, Either<A, B>> selected = Writer.unbox(value);
            if (selected.value().isRight()) {
                return new Writer<>(selected.written(), selected.value().right());
            }
            Writer<W, ? extends Function<A, B>> fn = Writer.unbox(function);
            return new Writer<>(
                    monoid.combine(selected.written(), fn.written()),
                    fn.value().apply(selected.value().left()));
        }

        /**
         * Evaluates one deferred writer branch and combines its output with the condition output.
         *
         * @param <A> the result type
         * @param condition the writer containing the condition
         * @param thenValue the deferred writer used for a true condition
         * @param elseValue the deferred writer used for a false condition
         * @return the selected result and accumulated output in encoded writer form
         */
        @Override
        public <A> App<Writer.Mu<W>, A> ifS(
                App<Writer.Mu<W>, Boolean> condition,
                Supplier<? extends App<Writer.Mu<W>, A>> thenValue,
                Supplier<? extends App<Writer.Mu<W>, A>> elseValue) {
            Writer<W, Boolean> test = Writer.unbox(condition);
            Supplier<? extends App<Writer.Mu<W>, A>> branch =
                    Boolean.TRUE.equals(test.value()) ? thenValue : elseValue;
            Writer<W, A> selected = Writer.unbox(branch.get());
            return new Writer<>(monoid.combine(test.written(), selected.written()), selected.value());
        }
    }
}

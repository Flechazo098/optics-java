package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.*;
import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.*;

/**
 * Provides alternative, error, and selective operations for virtual-thread streams.
 */
public enum VStreamAlternative implements MonadError<VStream.Mu, Throwable, VStream.InstanceMu>,
        MonadZero<VStream.Mu, VStream.InstanceMu>,
        Selective<VStream.Mu, VStream.InstanceMu> {
    /**
     * Provides the shared virtual-thread stream alternative.
     */
    INSTANCE;

    /**
     * Creates a singleton encoded stream.
     *
     * @param <A> the element type
     * @param value the sole element
     * @return the singleton stream in encoded form
     */
    @Override
    public <A> App<VStream.Mu, A> of(A value) {
        return VStream.of(value);
    }

    /**
     * Maps each element to an encoded stream and concatenates the results.
     *
     * @param <A> the source element type
     * @param <B> the result element type
     * @param f the stream-producing transformation
     * @param fa the source stream
     * @return the concatenated results in encoded stream form
     */
    @Override
    public <A, B> App<VStream.Mu, B> flatMap(
            Function<? super A, ? extends App<VStream.Mu, B>> f,
            App<VStream.Mu, A> fa) {
        return VStreamMonad.INSTANCE.flatMap(f, fa);
    }

    /**
     * Returns the empty encoded stream.
     *
     * @param <A> the element type
     * @return the empty stream in encoded form
     */
    @Override
    public <A> App<VStream.Mu, A> zero() {
        return VStream.empty();
    }

    /**
     * Concatenates two encoded stream alternatives.
     *
     * @param <A> the element type
     * @param first the first stream
     * @param second the deferred second stream
     * @return the concatenated stream in encoded form
     */
    @Override
    public <A> App<VStream.Mu, A> orElse(
            App<VStream.Mu, A> first,
            Supplier<? extends App<VStream.Mu, A>> second) {
        Objects.requireNonNull(second, "second");
        return VStream.concat(VStream.unbox(first), VStream.defer(() -> VStream.unbox(second.get())));
    }

    /**
     * Creates an encoded stream that fails when pulled.
     *
     * @param <A> the element type
     * @param error the failure cause
     * @return the failed stream in encoded form
     */
    @Override
    public <A> App<VStream.Mu, A> raiseError(Throwable error) {
        return VStream.fail(error);
    }

    /**
     * Recovers an encoded stream failure by selecting another stream.
     *
     * @param <A> the element type
     * @param value the source stream
     * @param handler the function selecting a replacement stream
     * @return the recovered stream in encoded form
     */
    @Override
    public <A> App<VStream.Mu, A> handleErrorWith(
            App<VStream.Mu, A> value,
            Function<? super Throwable, ? extends App<VStream.Mu, A>> handler) {
        Validation.function().validateHandleErrorWith(value, handler);
        return VStream.unbox(value).recoverWith(error ->
                VStream.unbox(Validation.function().requireNonNullResult(handler.apply(error), "handler", HANDLE_ERROR_WITH)));
    }

    /**
     * Resolves encoded either elements using encoded functions for left values.
     *
     * @param <A> the function argument type
     * @param <B> the result type
     * @param value the stream of branch values
     * @param function the stream of functions for left values
     * @return the selected results in encoded stream form
     */
    @Override
    public <A, B> App<VStream.Mu, B> select(
            App<VStream.Mu, Either<A, B>> value,
            App<VStream.Mu, ? extends Function<A, B>> function) {
        VStream<? extends Function<A, B>> functions = VStream.unbox(function);
        return VStream.unbox(value).flatMap(choice -> {
            Either<A, B> either = Validation.coreType().requireValue(choice, "select value", VStream.class, SELECT);
            if (either.isRight()) {
                return VStream.of(either.right());
            }
            return functions.map(fn -> Objects.requireNonNull(fn, "select function").apply(either.left()));
        });
    }

    /**
     * Selects a deferred stream branch for each encoded condition.
     *
     * @param <A> the result type
     * @param condition the stream of conditions
     * @param thenValue the deferred stream used for true conditions
     * @param elseValue the deferred stream used for false conditions
     * @return the selected elements in encoded stream form
     */
    @Override
    public <A> App<VStream.Mu, A> ifS(
            App<VStream.Mu, Boolean> condition,
            Supplier<? extends App<VStream.Mu, A>> thenValue,
            Supplier<? extends App<VStream.Mu, A>> elseValue) {
        Objects.requireNonNull(thenValue, "thenValue");
        Objects.requireNonNull(elseValue, "elseValue");
        return VStream.unbox(condition).flatMap(test ->
                VStream.unbox(test
                        ? Validation.function().requireNonNullResult(thenValue.get(), "thenValue", IF_S)
                        : Validation.function().requireNonNullResult(elseValue.get(), "elseValue", IF_S)));
    }
}

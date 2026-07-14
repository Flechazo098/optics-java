package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Monad;
import com.flechazo.hkt.util.validation.Validation;

import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.FLAT_MAP;

/**
 * Provides monadic sequencing for virtual-thread streams.
 */
public enum VStreamMonad implements Monad<VStream.Mu, VStream.InstanceMu> {
    /**
     * Provides the shared virtual-thread stream monad.
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
        Validation.function().validateFlatMap(f, fa);
        return VStream.unbox(fa).flatMap(value ->
                VStream.unbox(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP)));
    }
}

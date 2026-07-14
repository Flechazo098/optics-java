package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Functor;
import com.flechazo.hkt.util.validation.Validation;

import java.util.function.Function;

/**
 * Provides element mapping for virtual-thread streams.
 */
public enum VStreamFunctor implements Functor<VStream.Mu, VStream.InstanceMu> {
    /**
     * Provides the shared virtual-thread stream functor.
     */
    INSTANCE;

    /**
     * Transforms stream elements lazily while preserving encounter order.
     *
     * @param <A> the source element type
     * @param <B> the result element type
     * @param f the element transformation
     * @param fa the source stream
     * @return the transformed stream in encoded form
     */
    @Override
    public <A, B> App<VStream.Mu, B> map(Function<? super A, ? extends B> f, App<VStream.Mu, A> fa) {
        Validation.function().validateMap(f, fa);
        return VStream.unbox(fa).map(f);
    }
}

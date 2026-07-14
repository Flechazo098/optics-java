package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

/**
 * Provides applicative operations for virtual-thread streams.
 */
public enum VStreamApplicative implements Applicative<VStream.Mu, VStream.InstanceMu> {
    /**
     * Provides the shared virtual-thread stream applicative.
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
     * Transforms encoded stream elements lazily.
     *
     * @param <A> the source element type
     * @param <B> the result element type
     * @param f the element transformation
     * @param fa the source stream
     * @return the transformed stream in encoded form
     */
    @Override
    public <A, B> App<VStream.Mu, B> map(Function<? super A, ? extends B> f, App<VStream.Mu, A> fa) {
        return VStreamFunctor.INSTANCE.map(f, fa);
    }

    /**
     * Applies encoded stream functions to encoded stream values.
     *
     * @param <A> the argument type
     * @param <B> the result type
     * @param ff the stream of functions
     * @param fa the stream of arguments
     * @return the application results in encoded stream form
     */
    @Override
    public <A, B> App<VStream.Mu, B> ap(App<VStream.Mu, ? extends Function<A, B>> ff, App<VStream.Mu, A> fa) {
        Validation.kind().validateAp(ff, fa);
        VStream<? extends Function<A, B>> functions = VStream.unbox(ff);
        VStream<A> values = VStream.unbox(fa);
        return functions.flatMap(function -> {
            Function<A, B> apply = Objects.requireNonNull(function, "function");
            return values.map(apply);
        });
    }
}

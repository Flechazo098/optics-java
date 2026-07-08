package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Monad;
import com.flechazo.hkt.util.validation.Validation;

import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.FLAT_MAP;

public enum VStreamMonad implements Monad<VStream.Mu, VStream.InstanceMu> {
    INSTANCE;

    @Override
    public <A> App<VStream.Mu, A> of(A value) {
        return VStream.of(value);
    }

    @Override
    public <A, B> App<VStream.Mu, B> flatMap(
            Function<? super A, ? extends App<VStream.Mu, B>> f,
            App<VStream.Mu, A> fa) {
        Validation.function().validateFlatMap(f, fa);
        return VStream.unbox(fa).flatMap(value ->
                VStream.unbox(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP)));
    }
}

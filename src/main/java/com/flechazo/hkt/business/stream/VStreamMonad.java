package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Monad;

import java.util.Objects;
import java.util.function.Function;

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
        Objects.requireNonNull(f, "f");
        return VStream.unbox(fa).flatMap(value ->
                VStream.unbox(Objects.requireNonNull(f.apply(value), "flatMap result")));
    }
}

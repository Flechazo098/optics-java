package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Functor;

import java.util.Objects;
import java.util.function.Function;

public enum VStreamFunctor implements Functor<VStream.Mu, VStream.InstanceMu> {
    INSTANCE;

    @Override
    public <A, B> App<VStream.Mu, B> map(Function<? super A, ? extends B> f, App<VStream.Mu, A> fa) {
        Objects.requireNonNull(f, "f");
        return VStream.unbox(fa).map(f);
    }
}

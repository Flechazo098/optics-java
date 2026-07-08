package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Functor;
import com.flechazo.hkt.util.validation.Validation;

import java.util.function.Function;

public enum VStreamFunctor implements Functor<VStream.Mu, VStream.InstanceMu> {
    INSTANCE;

    @Override
    public <A, B> App<VStream.Mu, B> map(Function<? super A, ? extends B> f, App<VStream.Mu, A> fa) {
        Validation.function().validateMap(f, fa);
        return VStream.unbox(fa).map(f);
    }
}

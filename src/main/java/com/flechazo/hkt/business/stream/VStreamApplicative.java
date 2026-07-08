package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

public enum VStreamApplicative implements Applicative<VStream.Mu, VStream.InstanceMu> {
    INSTANCE;

    @Override
    public <A> App<VStream.Mu, A> of(A value) {
        return VStream.of(value);
    }

    @Override
    public <A, B> App<VStream.Mu, B> map(Function<? super A, ? extends B> f, App<VStream.Mu, A> fa) {
        return VStreamFunctor.INSTANCE.map(f, fa);
    }

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

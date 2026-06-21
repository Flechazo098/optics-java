package com.flechazo.optics.generated;

import com.flechazo.hkt.Functor;
import com.flechazo.hkt.App;
import com.flechazo.hkt.K1;
import com.flechazo.optics.Lens;

import java.util.function.Function;

public abstract class GeneratedLens<S, A> implements Lens<S, A> {
    @Override
    public <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Functor<F> functor) {
        return functor.map(value -> set(value, source), f.apply(get(source)));
    }
}

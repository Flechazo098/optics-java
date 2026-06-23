package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

import java.util.function.Function;

public interface Closed<P extends K2, Proof extends Closed.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {};
    }

    static <P extends K2, Proof extends Mu> Closed<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Closed<P, Proof>) proofBox;
    }

    <A, B, X> App2<P, Function<X, A>, Function<X, B>> closed(App2<P, A, B> value);
}

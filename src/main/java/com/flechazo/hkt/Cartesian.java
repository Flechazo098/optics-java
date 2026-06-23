package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

public interface Cartesian<P extends K2, Proof extends Cartesian.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {};
    }

    static <P extends K2, Proof extends Mu> Cartesian<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Cartesian<P, Proof>) proofBox;
    }

    <A, B, C> App2<P, Pair<A, C>, Pair<B, C>> first(App2<P, A, B> value);

    <A, B, C> App2<P, Pair<C, A>, Pair<C, B>> second(App2<P, A, B> value);
}

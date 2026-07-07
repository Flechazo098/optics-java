package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

public interface Cartesian<P extends K2, Proof extends Cartesian.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> Cartesian<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Cartesian<P, Proof>) proofBox;
    }

    <A, B, C> App2<P, Tuple2<A, C>, Tuple2<B, C>> first(App2<P, A, B> value);

    default <A, B, C> App2<P, Tuple2<C, A>, Tuple2<C, B>> second(App2<P, A, B> value) {
        return dimap(first(value), Tuple2::swap, Tuple2::swap);
    }
}

package com.flechazo.hkt;

import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.util.validation.Validation;

import com.google.common.reflect.TypeToken;

public interface ReCartesian<P extends K2, Proof extends ReCartesian.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> ReCartesian<P, Proof> unbox(App<Proof, P> proofBox) {
        return (ReCartesian<P, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, ReCartesian.class);
    }

    <A, B, C> App2<P, A, B> unfirst(App2<P, Tuple2<A, C>, Tuple2<B, C>> input);

    <A, B, C> App2<P, A, B> unsecond(App2<P, Tuple2<C, A>, Tuple2<C, B>> input);
}

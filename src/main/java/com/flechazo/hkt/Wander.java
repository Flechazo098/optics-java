package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

public interface Wander<P extends K2, Proof extends Wander.Mu> extends Traversing<P, Proof> {
    interface Mu extends Traversing.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {};
    }

    static <P extends K2, Proof extends Mu> Wander<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Wander<P, Proof>) proofBox;
    }
}

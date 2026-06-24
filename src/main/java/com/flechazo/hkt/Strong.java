package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

public interface Strong<P extends K2, Proof extends Strong.Mu> extends Cartesian<P, Proof> {
    interface Mu extends Cartesian.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> Strong<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Strong<P, Proof>) proofBox;
    }
}

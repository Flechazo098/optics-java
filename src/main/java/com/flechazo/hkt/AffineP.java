package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

public interface AffineP<P extends K2, Proof extends AffineP.Mu>
        extends Cartesian<P, Proof>, Cocartesian<P, Proof> {
    interface Mu extends Cartesian.Mu, Cocartesian.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> AffineP<P, Proof> unbox(App<Proof, P> proofBox) {
        return (AffineP<P, Proof>) proofBox;
    }
}

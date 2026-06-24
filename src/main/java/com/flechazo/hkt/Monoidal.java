package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

import java.util.function.Supplier;

public interface Monoidal<P extends K2, Proof extends Monoidal.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    <A, B, C, D> App2<P, Pair<A, C>, Pair<B, D>> par(
            App2<P, A, B> first,
            Supplier<App2<P, C, D>> second);

    App2<P, Unit, Unit> empty();
}

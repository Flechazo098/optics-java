package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

public interface Traversing<P extends K2, Proof extends Traversing.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {};
    }


    <T extends K1, A, B> App2<P, App<T, A>, App<T, B>> traversing(
            Traversable<T> traversable,
            App2<P, A, B> value);
}

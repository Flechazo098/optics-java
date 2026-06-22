package com.flechazo.hkt;

public interface Cocartesian<P extends K2> extends Profunctor<P> {
    <A, B, C> App2<P, Either<A, C>, Either<B, C>> left(App2<P, A, B> value);

    <A, B, C> App2<P, Either<C, A>, Either<C, B>> right(App2<P, A, B> value);
}

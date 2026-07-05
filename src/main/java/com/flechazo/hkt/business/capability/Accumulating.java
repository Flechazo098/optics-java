package com.flechazo.hkt.business.capability;

import com.flechazo.hkt.function.Function3;

import java.util.function.BiFunction;

public interface Accumulating<E, A> extends Composable<A> {
    <B, C> Accumulating<E, C> zipWithAccum(
            Accumulating<E, B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner);

    <B, C, D> Accumulating<E, D> zipWith3Accum(
            Accumulating<E, B> second,
            Accumulating<E, C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner);

    Accumulating<E, A> andAlso(Accumulating<E, ?> other);

    <B> Accumulating<E, B> andThen(Accumulating<E, B> other);
}

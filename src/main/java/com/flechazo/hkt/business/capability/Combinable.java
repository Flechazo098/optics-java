package com.flechazo.hkt.business.capability;

import com.flechazo.hkt.function.Function3;

import java.util.function.BiFunction;

public interface Combinable<A> extends Composable<A> {
    <B, C> Combinable<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner);

    default <B, C, D> Combinable<D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWith(second, Pair2::new)
                .zipWith(third, (pair, c) -> combiner.apply(pair.first(), pair.second(), c));
    }

    record Pair2<A, B>(A first, B second) {
    }
}

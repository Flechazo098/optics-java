package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.Function;

public final class TupleTraversals {
    private TupleTraversals() {
    }

    public static <A, B> Traversal<Tuple2<A, B>, A> first() {
        return Traversal.from(TupleTraversals.<A, A, B>pFirst());
    }

    public static <A, C, B> PTraversal<Tuple2<A, B>, Tuple2<C, B>, A, C> pFirst() {
        PTraversal<Tuple2<A, B>, Tuple2<C, B>, A, C> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Tuple2<C, B>> modifyF(
                    Function<A, App<F, C>> f, Tuple2<A, B> source, Applicative<F, ?> applicative) {
                return applicative.map(next -> new Tuple2<>(next, source.second()), f.apply(source.first()));
            }
        };
        return OpticPrograms.traversal(
                direct,
                OpticPrograms.structured("tupleFirstTraversal", null));
    }

    public static <A, B> Traversal<Tuple2<A, B>, B> second() {
        return Traversal.from(TupleTraversals.<A, B, B>pSecond());
    }

    public static <A, B, C> PTraversal<Tuple2<A, B>, Tuple2<A, C>, B, C> pSecond() {
        PTraversal<Tuple2<A, B>, Tuple2<A, C>, B, C> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Tuple2<A, C>> modifyF(
                    Function<B, App<F, C>> f, Tuple2<A, B> source, Applicative<F, ?> applicative) {
                return applicative.map(next -> new Tuple2<>(source.first(), next), f.apply(source.second()));
            }
        };
        return OpticPrograms.traversal(
                direct,
                OpticPrograms.structured("tupleSecondTraversal", null));
    }
}

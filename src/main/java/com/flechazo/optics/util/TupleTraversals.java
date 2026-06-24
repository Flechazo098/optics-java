package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Tuple2;
import com.flechazo.optics.Traversal;

import java.util.function.Function;

public final class TupleTraversals {
    private TupleTraversals() {
    }

    public static <A, B> Traversal<Tuple2<A, B>, Tuple2<A, B>, A, A> first() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Tuple2<A, B>> modifyF(
                    Function<A, App<F, A>> f, Tuple2<A, B> source, Applicative<F, ?> applicative) {
                return applicative.map(next -> new Tuple2<>(next, source.second()), f.apply(source.first()));
            }
        };
    }

    public static <A, B> Traversal<Tuple2<A, B>, Tuple2<A, B>, B, B> second() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Tuple2<A, B>> modifyF(
                    Function<B, App<F, B>> f, Tuple2<A, B> source, Applicative<F, ?> applicative) {
                return applicative.map(next -> new Tuple2<>(source.first(), next), f.apply(source.second()));
            }
        };
    }
}

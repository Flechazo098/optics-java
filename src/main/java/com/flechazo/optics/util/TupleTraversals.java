package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.Function;

/**
 * Provides traversals over the components of two-element tuples.
 */
public final class TupleTraversals {
    private TupleTraversals() {
    }

    /**
     * Creates a traversal that focuses the first tuple component.
     *
     * @param <A> the first component type
     * @param <B> the second component type
     * @return a traversal over the first component
     */
    public static <A, B> Traversal<Tuple2<A, B>, A> first() {
        return Traversal.from(TupleTraversals.pFirst());
    }

    /**
     * Creates a polymorphic traversal that focuses the first tuple component.
     *
     * @param <A> the source first component type
     * @param <C> the replacement first component type
     * @param <B> the unchanged second component type
     * @return a traversal over the first component
     */
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

    /**
     * Creates a traversal that focuses the second tuple component.
     *
     * @param <A> the first component type
     * @param <B> the second component type
     * @return a traversal over the second component
     */
    public static <A, B> Traversal<Tuple2<A, B>, B> second() {
        return Traversal.from(TupleTraversals.pSecond());
    }

    /**
     * Creates a polymorphic traversal that focuses the second tuple component.
     *
     * @param <A> the unchanged first component type
     * @param <B> the source second component type
     * @param <C> the replacement second component type
     * @return a traversal over the second component
     */
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

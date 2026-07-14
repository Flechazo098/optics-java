package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Try;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.Function;

/**
 * Provides traversals for successful {@link Try} values.
 */
public final class TryTraversals {
    private TryTraversals() {
    }

    /**
     * Creates a traversal that focuses the value of a successful computation.
     *
     * @param <A> the success value type
     * @return a traversal with no focus for a failed computation
     */
    public static <A> Traversal<Try<A>, A> success() {
        return Traversal.from(TryTraversals.pSuccess());
    }

    /**
     * Creates a polymorphic traversal that focuses the value of a successful computation.
     *
     * @param <A> the source success value type
     * @param <B> the replacement success value type
     * @return a traversal that preserves failed computations
     */
    public static <A, B> PTraversal<Try<A>, Try<B>, A, B> pSuccess() {
        PTraversal<Try<A>, Try<B>, A, B> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Try<B>> modifyF(
                    Function<A, App<F, B>> f, Try<A> source, Applicative<F, ?> applicative) {
                return source.isSuccess()
                        ? applicative.map(Try::success, f.apply(source.get()))
                        : applicative.of(Try.failure(source.cause()));
            }
        };
        return OpticPrograms.traversal(
                direct,
                OpticPrograms.structured("trySuccessTraversal", null));
    }
}

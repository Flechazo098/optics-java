package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Try;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.Function;

public final class TryTraversals {
    private TryTraversals() {
    }

    public static <A> Traversal<Try<A>, A> success() {
        return Traversal.from(TryTraversals.<A, A>pSuccess());
    }

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

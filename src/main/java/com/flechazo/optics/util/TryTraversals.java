package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Try;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.Function;

public final class TryTraversals {
    private TryTraversals() {
    }

    public static <A> PTraversal<Try<A>, Try<A>, A, A> success() {
        PTraversal<Try<A>, Try<A>, A, A> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Try<A>> modifyF(
                    Function<A, App<F, A>> f, Try<A> source, Applicative<F, ?> applicative) {
                return source.isSuccess()
                        ? applicative.map(Try::success, f.apply(source.get()))
                        : applicative.of(source);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.structured("trySuccessTraversal", null));
    }
}

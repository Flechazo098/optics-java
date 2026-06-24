package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Try;
import com.flechazo.optics.Traversal;

import java.util.function.Function;

public final class TryTraversals {
    private TryTraversals() {
    }

    public static <A> Traversal<Try<A>, Try<A>, A, A> success() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Try<A>> modifyF(
                    Function<A, App<F, A>> f, Try<A> source, Applicative<F, ?> applicative) {
                return source.isSuccess()
                        ? applicative.map(Try::success, f.apply(source.get()))
                        : applicative.of(source);
            }
        };
    }
}

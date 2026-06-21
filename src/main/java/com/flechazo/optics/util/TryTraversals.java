package com.flechazo.optics.util;

import com.flechazo.hkt.*;
import com.flechazo.optics.Traversal;

import java.util.function.Function;

public final class TryTraversals {
    private TryTraversals() {
    }

    public static <A> Traversal<Try<A>, A> success() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Try<A>> modifyF(
                    Function<A, App<F, A>> f, Try<A> source, Applicative<F> applicative) {
                return source.isSuccess()
                        ? applicative.map(Try::success, f.apply(source.get()))
                        : applicative.of(source);
            }
        };
    }
}

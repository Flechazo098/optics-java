package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Validated;
import com.flechazo.optics.Traversal;

import java.util.function.Function;

public final class ValidatedTraversals {
    private ValidatedTraversals() {
    }

    public static <E, A> Traversal<Validated<E, A>, A> valid() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Validated<E, A>> modifyF(
                    Function<A, App<F, A>> f, Validated<E, A> source, Applicative<F, ?> applicative) {
                return source.isValid()
                        ? applicative.map(Validated::valid, f.apply(source.value()))
                        : applicative.of(source);
            }
        };
    }
}
 
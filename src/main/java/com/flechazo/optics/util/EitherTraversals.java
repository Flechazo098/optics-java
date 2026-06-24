package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.optics.Traversal;

import java.util.function.Function;

public final class EitherTraversals {
    private EitherTraversals() {
    }

    public static <L, R> Traversal<Either<L, R>, R> right() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Either<L, R>> modifyF(
                    Function<R, App<F, R>> f, Either<L, R> source, Applicative<F, ?> applicative) {
                return source.isRight()
                        ? applicative.map(Either::right, f.apply(source.right()))
                        : applicative.of(source);
            }
        };
    }

    public static <L, R> Traversal<Either<L, R>, L> left() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Either<L, R>> modifyF(
                    Function<L, App<F, L>> f, Either<L, R> source, Applicative<F, ?> applicative) {
                return source.isLeft()
                        ? applicative.map(Either::left, f.apply(source.left()))
                        : applicative.of(source);
            }
        };
    }
}

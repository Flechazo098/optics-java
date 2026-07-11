package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.function.Function;

public final class EitherTraversals {
    private EitherTraversals() {
    }

    public static <L, R> PTraversal<Either<L, R>, Either<L, R>, R, R> right() {
        PTraversal<Either<L, R>, Either<L, R>, R, R> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Either<L, R>> modifyF(
                    Function<R, App<F, R>> f, Either<L, R> source, Applicative<F, ?> applicative) {
                return source.isRight()
                        ? applicative.map(Either::right, f.apply(source.right()))
                        : applicative.of(source);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.structured("eitherRightTraversal", null));
    }

    public static <L, R> PTraversal<Either<L, R>, Either<L, R>, R, R> right(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return right(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> PTraversal<Either<L, R>, Either<L, R>, R, R> right(
            Type<L> leftType,
            Type<R> rightType) {
        return OpticMetadata.optic(
                right(), Maybe.some(PointFreeOptic.right(leftType, rightType)));
    }

    public static <L, R> PTraversal<Either<L, R>, Either<L, R>, L, L> left() {
        PTraversal<Either<L, R>, Either<L, R>, L, L> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Either<L, R>> modifyF(
                    Function<L, App<F, L>> f, Either<L, R> source, Applicative<F, ?> applicative) {
                return source.isLeft()
                        ? applicative.map(Either::left, f.apply(source.left()))
                        : applicative.of(source);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.structured("eitherLeftTraversal", null));
    }

    public static <L, R> PTraversal<Either<L, R>, Either<L, R>, L, L> left(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return left(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> PTraversal<Either<L, R>, Either<L, R>, L, L> left(
            Type<L> leftType,
            Type<R> rightType) {
        return OpticMetadata.optic(
                left(), Maybe.some(PointFreeOptic.left(leftType, rightType)));
    }
}

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
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.function.Function;

public final class EitherTraversals {
    private EitherTraversals() {
    }

    public static <L, R> Traversal<Either<L, R>, R> right() {
        return Traversal.from(EitherTraversals.<L, R, R>pRight());
    }

    public static <L, R, B> PTraversal<Either<L, R>, Either<L, B>, R, B> pRight() {
        PTraversal<Either<L, R>, Either<L, B>, R, B> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Either<L, B>> modifyF(
                    Function<R, App<F, B>> f, Either<L, R> source, Applicative<F, ?> applicative) {
                return source.isRight()
                        ? applicative.map(Either::right, f.apply(source.right()))
                        : applicative.of(Either.left(source.left()));
            }
        };
        return OpticPrograms.traversal(
                direct,
                OpticPrograms.structured("eitherRightTraversal", null));
    }

    public static <L, R> Traversal<Either<L, R>, R> right(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return right(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> Traversal<Either<L, R>, R> right(
            Type<L> leftType,
            Type<R> rightType) {
        return Traversal.from(EitherTraversals.<L, R, R>pRight(leftType, rightType, rightType));
    }

    public static <L, R, B> PTraversal<Either<L, R>, Either<L, B>, R, B> pRight(
            TypeToken<L> leftType,
            TypeToken<R> rightType,
            TypeToken<B> targetRightType) {
        return pRight(
                Types.witness(leftType), Types.witness(rightType), Types.witness(targetRightType));
    }

    public static <L, R, B> PTraversal<Either<L, R>, Either<L, B>, R, B> pRight(
            Type<L> leftType,
            Type<R> rightType,
            Type<B> targetRightType) {
        return OpticMetadata.optic(
                EitherTraversals.<L, R, B>pRight(),
                Maybe.some(PointFreeOptic.right(leftType, rightType, targetRightType)));
    }

    public static <L, R> Traversal<Either<L, R>, L> left() {
        return Traversal.from(EitherTraversals.<L, L, R>pLeft());
    }

    public static <L, M, R> PTraversal<Either<L, R>, Either<M, R>, L, M> pLeft() {
        PTraversal<Either<L, R>, Either<M, R>, L, M> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Either<M, R>> modifyF(
                    Function<L, App<F, M>> f, Either<L, R> source, Applicative<F, ?> applicative) {
                return source.isLeft()
                        ? applicative.map(Either::left, f.apply(source.left()))
                        : applicative.of(Either.right(source.right()));
            }
        };
        return OpticPrograms.traversal(
                direct,
                OpticPrograms.structured("eitherLeftTraversal", null));
    }

    public static <L, R> Traversal<Either<L, R>, L> left(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return left(Types.witness(leftType), Types.witness(rightType));
    }

    public static <L, R> Traversal<Either<L, R>, L> left(
            Type<L> leftType,
            Type<R> rightType) {
        return Traversal.from(EitherTraversals.<L, L, R>pLeft(leftType, leftType, rightType));
    }

    public static <L, M, R> PTraversal<Either<L, R>, Either<M, R>, L, M> pLeft(
            TypeToken<L> leftType,
            TypeToken<M> targetLeftType,
            TypeToken<R> rightType) {
        return pLeft(
                Types.witness(leftType), Types.witness(targetLeftType), Types.witness(rightType));
    }

    public static <L, M, R> PTraversal<Either<L, R>, Either<M, R>, L, M> pLeft(
            Type<L> leftType,
            Type<M> targetLeftType,
            Type<R> rightType) {
        return OpticMetadata.optic(
                EitherTraversals.<L, M, R>pLeft(),
                Maybe.some(PointFreeOptic.left(leftType, targetLeftType, rightType)));
    }
}

package com.flechazo.optics.util;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.function.Function;

/**
 * Provides traversals over the alternatives of {@link Either} values.
 */
public final class EitherTraversals {
    private EitherTraversals() {
    }

    /**
     * Creates a traversal that focuses the right alternative.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @return a traversal with no focus for a left value
     */
    public static <L, R> Traversal<Either<L, R>, R> right() {
        return Traversal.from(EitherTraversals.pRight());
    }

    /**
     * Creates a polymorphic traversal that focuses the right alternative.
     *
     * @param <L> the unchanged left value type
     * @param <R> the source right value type
     * @param <B> the replacement right value type
     * @return a traversal that preserves left values
     */
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

    /**
     * Creates a typed traversal that focuses the right alternative.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param leftType the runtime description of the left value type
     * @param rightType the runtime description of the right value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <L, R> Traversal<Either<L, R>, R> right(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return right(Types.witness(leftType), Types.witness(rightType));
    }

    /**
     * Creates a typed traversal that focuses the right alternative.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param leftType the runtime witness for the left value type
     * @param rightType the runtime witness for the right value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <L, R> Traversal<Either<L, R>, R> right(
            Type<L> leftType,
            Type<R> rightType) {
        return Traversal.from(EitherTraversals.pRight(leftType, rightType, rightType));
    }

    /**
     * Creates a typed polymorphic traversal that focuses the right alternative.
     *
     * @param <L> the unchanged left value type
     * @param <R> the source right value type
     * @param <B> the replacement right value type
     * @param leftType the runtime description of the left value type
     * @param rightType the runtime description of the source right value type
     * @param targetRightType the runtime description of the replacement right value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <L, R, B> PTraversal<Either<L, R>, Either<L, B>, R, B> pRight(
            TypeToken<L> leftType,
            TypeToken<R> rightType,
            TypeToken<B> targetRightType) {
        return pRight(
                Types.witness(leftType), Types.witness(rightType), Types.witness(targetRightType));
    }

    /**
     * Creates a typed polymorphic traversal that focuses the right alternative.
     *
     * @param <L> the unchanged left value type
     * @param <R> the source right value type
     * @param <B> the replacement right value type
     * @param leftType the runtime witness for the left value type
     * @param rightType the runtime witness for the source right value type
     * @param targetRightType the runtime witness for the replacement right value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <L, R, B> PTraversal<Either<L, R>, Either<L, B>, R, B> pRight(
            Type<L> leftType,
            Type<R> rightType,
            Type<B> targetRightType) {
        return OpticMetadata.optic(
                EitherTraversals.pRight(),
                Maybe.some(PointFreeOptic.right(leftType, rightType, targetRightType)));
    }

    /**
     * Creates a traversal that focuses the left alternative.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @return a traversal with no focus for a right value
     */
    public static <L, R> Traversal<Either<L, R>, L> left() {
        return Traversal.from(EitherTraversals.pLeft());
    }

    /**
     * Creates a polymorphic traversal that focuses the left alternative.
     *
     * @param <L> the source left value type
     * @param <M> the replacement left value type
     * @param <R> the unchanged right value type
     * @return a traversal that preserves right values
     */
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

    /**
     * Creates a typed traversal that focuses the left alternative.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param leftType the runtime description of the left value type
     * @param rightType the runtime description of the right value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <L, R> Traversal<Either<L, R>, L> left(
            TypeToken<L> leftType,
            TypeToken<R> rightType) {
        return left(Types.witness(leftType), Types.witness(rightType));
    }

    /**
     * Creates a typed traversal that focuses the left alternative.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param leftType the runtime witness for the left value type
     * @param rightType the runtime witness for the right value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <L, R> Traversal<Either<L, R>, L> left(
            Type<L> leftType,
            Type<R> rightType) {
        return Traversal.from(EitherTraversals.pLeft(leftType, leftType, rightType));
    }

    /**
     * Creates a typed polymorphic traversal that focuses the left alternative.
     *
     * @param <L> the source left value type
     * @param <M> the replacement left value type
     * @param <R> the unchanged right value type
     * @param leftType the runtime description of the source left value type
     * @param targetLeftType the runtime description of the replacement left value type
     * @param rightType the runtime description of the right value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <L, M, R> PTraversal<Either<L, R>, Either<M, R>, L, M> pLeft(
            TypeToken<L> leftType,
            TypeToken<M> targetLeftType,
            TypeToken<R> rightType) {
        return pLeft(
                Types.witness(leftType), Types.witness(targetLeftType), Types.witness(rightType));
    }

    /**
     * Creates a typed polymorphic traversal that focuses the left alternative.
     *
     * @param <L> the source left value type
     * @param <M> the replacement left value type
     * @param <R> the unchanged right value type
     * @param leftType the runtime witness for the source left value type
     * @param targetLeftType the runtime witness for the replacement left value type
     * @param rightType the runtime witness for the right value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <L, M, R> PTraversal<Either<L, R>, Either<M, R>, L, M> pLeft(
            Type<L> leftType,
            Type<M> targetLeftType,
            Type<R> rightType) {
        return OpticMetadata.optic(
                EitherTraversals.pLeft(),
                Maybe.some(PointFreeOptic.left(leftType, targetLeftType, rightType)));
    }
}

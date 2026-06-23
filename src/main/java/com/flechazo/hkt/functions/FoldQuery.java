package com.flechazo.hkt.functions;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;
import com.flechazo.optics.Fold;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class FoldQuery<S, A, M, R> implements Function<S, R>, PointFree<Function<S, R>> {
    private final Fold<S, A> fold;
    private final Monoid<M> monoid;
    private final Function<? super A, ? extends M> mapper;
    private final Function<? super M, ? extends R> finish;
    private final Type<S> sourceType;
    private final Type<R> resultType;

    private FoldQuery(
            Fold<S, A> fold,
            Monoid<M> monoid,
            Function<? super A, ? extends M> mapper,
            Function<? super M, ? extends R> finish,
            Type<S> sourceType,
            Type<R> resultType) {
        this.fold = Objects.requireNonNull(fold, "fold");
        this.monoid = Objects.requireNonNull(monoid, "monoid");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.finish = Objects.requireNonNull(finish, "finish");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.resultType = Objects.requireNonNull(resultType, "resultType");
    }

    public static <S, A, M> FoldQuery<S, A, M, M> foldMap(
            Fold<S, A> fold, Monoid<M> monoid, Function<? super A, ? extends M> mapper) {
        return new FoldQuery<>(
                fold,
                monoid,
                mapper,
                Function.identity(),
                Types.variable("FoldSource"),
                Types.variable("FoldResult"));
    }

    public static <S, A, M> FoldQuery<S, A, M, M> foldMap(
            Fold<S, A> fold,
            Monoid<M> monoid,
            Function<? super A, ? extends M> mapper,
            TypeToken<S> sourceType,
            TypeToken<M> resultType) {
        return foldMap(fold, monoid, mapper, Types.witness(sourceType), Types.witness(resultType));
    }

    public static <S, A, M> FoldQuery<S, A, M, M> foldMap(
            Fold<S, A> fold,
            Monoid<M> monoid,
            Function<? super A, ? extends M> mapper,
            Type<S> sourceType,
            Type<M> resultType) {
        return new FoldQuery<>(
                fold,
                monoid,
                mapper,
                Function.identity(),
                sourceType,
                resultType);
    }

    public R run(S source) {
        return finish.apply(fold.foldMap(monoid, mapper, source));
    }

    @Override
    public Function<S, R> eval() {
        return this::run;
    }

    @Override
    public R apply(S source) {
        return run(source);
    }

    public <T> FoldQuery<S, A, M, T> mapResult(Function<? super R, ? extends T> f) {
        Objects.requireNonNull(f, "f");
        return new FoldQuery<>(
                fold,
                monoid,
                mapper,
                value -> f.apply(finish.apply(value)),
                sourceType,
                Types.variable("FoldResult"));
    }

    public <N, T> FoldQuery<S, A, Pair<M, N>, Pair<R, T>> zip(FoldQuery<S, A, N, T> other) {
        Type<Pair<R, T>> zippedResultType = Types.and(resultType, other.resultType);
        return zipWith(other, Pair::of, zippedResultType);
    }

    public <N, T, U> FoldQuery<S, A, Pair<M, N>, U> zipWith(
            FoldQuery<S, A, N, T> other,
            BiFunction<? super R, ? super T, ? extends U> combineResult) {
        return zipWith(other, combineResult, Types.variable("FoldZipResult"));
    }

    private <N, T, U> FoldQuery<S, A, Pair<M, N>, U> zipWith(
            FoldQuery<S, A, N, T> other,
            BiFunction<? super R, ? super T, ? extends U> combineResult,
            Type<U> zippedResultType) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combineResult, "combineResult");
        Objects.requireNonNull(zippedResultType, "zippedResultType");
        if (fold != other.fold) {
            throw new IllegalArgumentException("Cannot fuse fold queries from different Fold instances");
        }
        Type<S> zippedSourceType = compatibleSourceType(other);
        return new FoldQuery<>(
                fold,
                Monoid.product(monoid, other.monoid),
                value -> Pair.of(mapper.apply(value), other.mapper.apply(value)),
                pair -> combineResult.apply(finish.apply(pair.first()), other.finish.apply(pair.second())),
                zippedSourceType,
                zippedResultType);
    }

    public Fold<S, A> fold() {
        return fold;
    }

    @Override
    public Type<Function<S, R>> type() {
        return Types.function(sourceType, resultType);
    }

    private Type<S> compatibleSourceType(FoldQuery<S, A, ?, ?> other) {
        if (!PointFreeTypes.compatible(sourceType, other.sourceType)) {
            throw new IllegalArgumentException("fold query source type mismatch: "
                    + sourceType + ", " + other.sourceType);
        }
        return sourceType;
    }
}

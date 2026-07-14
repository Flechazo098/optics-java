package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.Fold;
import com.flechazo.optics.internal.OpticMetadata;
import com.google.common.reflect.TypeToken;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

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
        Fold<S, A> lowered = lowerFold(fold);
        return new FoldQuery<>(
                lowered,
                monoid,
                mapper,
                Function.identity(),
                sourceType(lowered, Types.variable("FoldSource")),
                Types.variable("FoldResult"));
    }

    public static <S, A> FoldQuery<S, A, Maybe<A>, Maybe<A>> preview(Fold<S, A> fold) {
        return first(fold);
    }

    public static <S, A> FoldQuery<S, A, Maybe<A>, Maybe<A>> first(Fold<S, A> fold) {
        Fold<S, A> lowered = lowerFold(fold);
        return new FoldQuery<>(
                lowered,
                firstMaybeMonoid(),
                Maybe::some,
                Function.identity(),
                sourceType(lowered, Types.variable("FoldSource")),
                Types.maybe(focusType(lowered, Types.variable("FoldFocus"))));
    }

    public static <S, A> FoldQuery<S, A, Integer, Integer> count(Fold<S, A> fold) {
        Fold<S, A> lowered = lowerFold(fold);
        return new FoldQuery<>(
                lowered,
                Monoid.of(0, Integer::sum),
                ignored -> 1,
                Function.identity(),
                sourceType(lowered, Types.variable("FoldSource")),
                Types.witness(Integer.class));
    }

    public static <S, A> FoldQuery<S, A, Boolean, Boolean> any(
            Fold<S, A> fold,
            Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Fold<S, A> lowered = lowerFold(fold);
        return new FoldQuery<>(
                lowered,
                Monoid.of(false, Boolean::logicalOr),
                predicate::test,
                Function.identity(),
                sourceType(lowered, Types.variable("FoldSource")),
                Types.witness(Boolean.class));
    }

    public static <S, A> FoldQuery<S, A, Boolean, Boolean> all(
            Fold<S, A> fold,
            Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Fold<S, A> lowered = lowerFold(fold);
        return new FoldQuery<>(
                lowered,
                Monoid.of(true, Boolean::logicalAnd),
                predicate::test,
                Function.identity(),
                sourceType(lowered, Types.variable("FoldSource")),
                Types.witness(Boolean.class));
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
        Fold<S, A> lowered = lowerFold(fold);
        return new FoldQuery<>(
                lowered,
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

    public <N, T> FoldQuery<S, A, Tuple2<M, N>, Tuple2<R, T>> zip(FoldQuery<S, A, N, T> other) {
        Type<Tuple2<R, T>> zippedResultType = Types.and(resultType, other.resultType);
        return zipWith(other, Tuple2::of, zippedResultType);
    }

    public <N, T, U> FoldQuery<S, A, Tuple2<M, N>, U> zipWith(
            FoldQuery<S, A, N, T> other,
            BiFunction<? super R, ? super T, ? extends U> combineResult) {
        return zipWith(other, combineResult, Types.variable("FoldZipResult"));
    }

    private <N, T, U> FoldQuery<S, A, Tuple2<M, N>, U> zipWith(
            FoldQuery<S, A, N, T> other,
            BiFunction<? super R, ? super T, ? extends U> combineResult,
            Type<U> zippedResultType) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combineResult, "combineResult");
        Objects.requireNonNull(zippedResultType, "zippedResultType");
        if (!sameFold(other)) {
            throw new IllegalArgumentException("Cannot fuse fold queries from different Fold instances");
        }
        Type<S> zippedSourceType = compatibleSourceType(other);
        return new FoldQuery<>(
                fold,
                Monoid.product(monoid, other.monoid),
                value -> Tuple2.of(mapper.apply(value), other.mapper.apply(value)),
                tuple -> combineResult.apply(finish.apply(tuple.first()), other.finish.apply(tuple.second())),
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

    private boolean sameFold(FoldQuery<S, A, ?, ?> other) {
        if (fold == other.fold) {
            return true;
        }
        if (fold instanceof PointFreeFold<?, ?> left && other.fold instanceof PointFreeFold<?, ?> right) {
            return left.sameFold(right);
        }
        return false;
    }

    private static <S, A> Fold<S, A> lowerFold(Fold<S, A> fold) {
        return OpticMetadata.<S, A>fold(fold).<Fold<S, A>>map(typed -> typed).orElse(fold);
    }

    private static <S, A> Type<S> sourceType(Fold<S, A> fold, Type<S> fallback) {
        if (fold instanceof PointFreeFold<S, A> typed) {
            return typed.sourceType();
        }
        return fallback;
    }

    private static <S, A> Type<A> focusType(Fold<S, A> fold, Type<A> fallback) {
        if (fold instanceof PointFreeFold<S, A> typed) {
            return typed.focusType();
        }
        return fallback;
    }

    private static <A> Monoid<Maybe<A>> firstMaybeMonoid() {
        return Monoid.of(Maybe.none(), (left, right) -> left.isDefined() ? left : right);
    }
}

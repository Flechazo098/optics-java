package com.flechazo.hkt.functions;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Pair;
import com.flechazo.optics.Fold;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class FoldQuery<S, A, M, R> implements Function<S, R> {
    private final Fold<S, A> fold;
    private final Monoid<M> monoid;
    private final Function<? super A, ? extends M> mapper;
    private final Function<? super M, ? extends R> finish;

    private FoldQuery(
            Fold<S, A> fold,
            Monoid<M> monoid,
            Function<? super A, ? extends M> mapper,
            Function<? super M, ? extends R> finish) {
        this.fold = Objects.requireNonNull(fold, "fold");
        this.monoid = Objects.requireNonNull(monoid, "monoid");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.finish = Objects.requireNonNull(finish, "finish");
    }

    public static <S, A, M> FoldQuery<S, A, M, M> foldMap(
            Fold<S, A> fold, Monoid<M> monoid, Function<? super A, ? extends M> mapper) {
        return new FoldQuery<>(fold, monoid, mapper, Function.identity());
    }

    public R run(S source) {
        return finish.apply(fold.foldMap(monoid, mapper, source));
    }

    @Override
    public R apply(S source) {
        return run(source);
    }

    public <T> FoldQuery<S, A, M, T> mapResult(Function<? super R, ? extends T> f) {
        Objects.requireNonNull(f, "f");
        return new FoldQuery<>(fold, monoid, mapper, value -> f.apply(finish.apply(value)));
    }

    public <N, T> FoldQuery<S, A, Pair<M, N>, Pair<R, T>> zip(FoldQuery<S, A, N, T> other) {
        return zipWith(other, Pair::of);
    }

    public <N, T, U> FoldQuery<S, A, Pair<M, N>, U> zipWith(
            FoldQuery<S, A, N, T> other,
            BiFunction<? super R, ? super T, ? extends U> combineResult) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(combineResult, "combineResult");
        if (fold != other.fold) {
            throw new IllegalArgumentException("Cannot fuse fold queries from different Fold instances");
        }
        return new FoldQuery<>(
                fold,
                Monoid.product(monoid, other.monoid),
                value -> Pair.of(mapper.apply(value), other.mapper.apply(value)),
                pair -> combineResult.apply(finish.apply(pair.first()), other.finish.apply(pair.second())));
    }

    public Fold<S, A> fold() {
        return fold;
    }
}

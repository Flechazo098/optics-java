package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Fold<S, A> {
    <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source);

    default List<A> getAll(S source) {
        ArrayList<A> values = new ArrayList<>();
        foldMap(
                Monoid.of(Unit.INSTANCE, (left, right) -> Unit.INSTANCE),
                value -> {
                    values.add(value);
                    return Unit.INSTANCE;
                },
                source);
        return Collections.unmodifiableList(values);
    }

    default Maybe<A> preview(S source) {
        return foldMap(firstMaybeMonoid(), Maybe::some, source);
    }

    default Maybe<A> find(Predicate<? super A> predicate, S source) {
        return foldMap(
                firstMaybeMonoid(),
                value -> predicate.test(value) ? Maybe.some(value) : Maybe.none(),
                source);
    }

    default A firstOrElse(A defaultValue, S source) {
        return preview(source).orElse(defaultValue);
    }

    default A firstOrElseGet(Supplier<? extends A> defaultValue, S source) {
        return preview(source).orElseGet(defaultValue);
    }

    default int length(S source) {
        return foldMap(Monoid.of(0, Integer::sum), ignored -> 1, source);
    }

    default boolean isEmpty(S source) {
        return length(source) == 0;
    }

    default boolean exists(Predicate<? super A> predicate, S source) {
        return foldMap(Monoid.of(false, Boolean::logicalOr), predicate::test, source);
    }

    default boolean all(Predicate<? super A> predicate, S source) {
        return foldMap(Monoid.of(true, Boolean::logicalAnd), predicate::test, source);
    }

    default <M, N, R> R foldMap2(
            Monoid<M> firstMonoid,
            Function<? super A, ? extends M> first,
            Monoid<N> secondMonoid,
            Function<? super A, ? extends N> second,
            BiFunction<? super M, ? super N, ? extends R> combineResult,
            S source) {
        Tuple2<M, N> folded =
                foldMap(
                        Monoid.product(firstMonoid, secondMonoid),
                        value -> Tuple2.of(first.apply(value), second.apply(value)),
                        source);
        return combineResult.apply(folded.first(), folded.second());
    }

    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        Fold<S, A> self = this;
        Fold<S, B> composed = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super B, ? extends M> f, S source) {
                return self.foldMap(monoid, value -> other.foldMap(monoid, f, value), source);
            }
        };
        Fold<S, B> typed = OpticMetadata.fold(
                composed,
                OpticMetadata.<S, A>fold(self)
                        .flatMap(left -> OpticMetadata.<A, B>fold(other).map(left::andThen)));
        return OpticPrograms.fold(typed, OpticPrograms.compose(self, other));
    }

    default <B> Fold<S, B> andThen(PPrism<A, A, B, B> prism) {
        return andThen(prism.asFold());
    }

    default <B> Fold<S, B> andThen(PLens<A, A, B, B> lens) {
        return andThen(lens.asFold());
    }

    default <B> Fold<S, B> andThen(PAffine<A, A, B, B> affine) {
        return andThen(affine.asFold());
    }

    default <B> Fold<S, B> andThen(PTraversal<A, A, B, B> traversal) {
        return andThen(traversal.asFold());
    }

    default Fold<S, A> at(int index) {
        Fold<S, A> self = this;
        Fold<S, A> direct = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                if (index < 0) {
                    return monoid.empty();
                }
                int[] current = {0};
                return self.foldMap(
                        monoid,
                        value -> current[0]++ == index ? f.apply(value) : monoid.empty(),
                        source);
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.structured("foldAt", index));
    }

    default Fold<S, A> filtered(Predicate<? super A> predicate) {
        Fold<S, A> self = this;
        Fold<S, A> filtered = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return self.foldMap(monoid, value -> predicate.test(value) ? f.apply(value) : monoid.empty(), source);
            }
        };
        Fold<S, A> typed = OpticMetadata.fold(
                filtered,
                OpticMetadata.<S, A>fold(self)
                        .map(fold -> (PointFreeFold<S, A>) fold.filtered(predicate)));
        return OpticPrograms.fold(typed, OpticPrograms.structured("filteredFold", null));
    }

    default <B> Fold<S, A> filterBy(Fold<A, B> query, Predicate<? super B> predicate) {
        Fold<S, A> self = this;
        Fold<S, A> direct = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return self.foldMap(
                        monoid,
                        value -> query.exists(predicate, value) ? f.apply(value) : monoid.empty(),
                        source);
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.structured("filterByFold", null));
    }

    default Fold<S, A> plus(Fold<S, A> other) {
        Fold<S, A> self = this;
        Fold<S, A> combined = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return monoid.combine(self.foldMap(monoid, f, source), other.foldMap(monoid, f, source));
            }
        };
        Fold<S, A> typed = OpticMetadata.fold(
                combined,
                OpticMetadata.<S, A>fold(self)
                        .flatMap(left -> OpticMetadata.<S, A>fold(other)
                                .map(right -> (PointFreeFold<S, A>) left.plus(right))));
        return OpticPrograms.fold(typed, OpticPrograms.structured("sumFold", null));
    }

    static <S, A> Fold<S, A> empty() {
        Fold<S, A> direct = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return monoid.empty();
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.structured("emptyFold", null));
    }

    static <S, A> Fold<S, A> of(Function<? super S, ? extends Iterable<? extends A>> getAll) {
        Fold<S, A> direct = iterableFold(getAll);
        return OpticPrograms.fold(direct, OpticPrograms.opaque("fold", null));
    }

    static <S, A> Fold<S, A> of(
            FoldGetter<? super S, A> getAll) {
        Fold<S, A> direct = iterableFold(getAll);
        return LambdaLifter.fold(direct, getAll);
    }

    private static <S, A> Fold<S, A> iterableFold(
            Function<? super S, ? extends Iterable<? extends A>> getAll) {
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                M result = monoid.empty();
                for (A value : getAll.apply(source)) {
                    result = monoid.combine(result, f.apply(value));
                }
                return result;
            }

            @Override
            public Maybe<A> preview(S source) {
                for (A value : getAll.apply(source)) {
                    return Maybe.some(value);
                }
                return Maybe.none();
            }

            @Override
            public Maybe<A> find(Predicate<? super A> predicate, S source) {
                for (A value : getAll.apply(source)) {
                    if (predicate.test(value)) {
                        return Maybe.some(value);
                    }
                }
                return Maybe.none();
            }

            @Override
            public boolean exists(Predicate<? super A> predicate, S source) {
                for (A value : getAll.apply(source)) {
                    if (predicate.test(value)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean all(Predicate<? super A> predicate, S source) {
                for (A value : getAll.apply(source)) {
                    if (!predicate.test(value)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    static <K, V> Fold<Map<K, V>, K> mapKeys() {
        return Fold.of(Map::keySet);
    }

    static <K, V> Fold<Map<K, V>, V> mapValues() {
        return Fold.of(Map::values);
    }

    static <K, V> Fold<Map<K, V>, Map.Entry<K, V>> mapEntries() {
        return Fold.of(Map::entrySet);
    }

    @SafeVarargs
    static <S, A> Fold<S, A> sum(Fold<S, A> first, Fold<S, A>... rest) {
        Fold<S, A> result = first;
        for (Fold<S, A> fold : rest) {
            result = result.plus(fold);
        }
        return result;
    }

    private static <A> Monoid<Maybe<A>> firstMaybeMonoid() {
        return Monoid.of(Maybe.none(), (left, right) -> left.isDefined() ? left : right);
    }
}

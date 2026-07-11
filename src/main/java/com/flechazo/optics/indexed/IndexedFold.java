package com.flechazo.optics.indexed;

import com.flechazo.hkt.*;
import com.flechazo.optics.Fold;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public interface IndexedFold<I, S, A> {
    <M> M ifoldMap(Monoid<M> monoid, BiFunction<? super I, ? super A, ? extends M> f, S source);

    default List<Tuple2<I, A>> toIndexedList(S source) {
        ArrayList<Tuple2<I, A>> values = new ArrayList<>();
        ifoldMap(
                Monoid.of(Unit.INSTANCE, (left, right) -> Unit.INSTANCE),
                (index, value) -> {
                    values.add(Tuple2.of(index, value));
                    return Unit.INSTANCE;
                },
                source);
        return values;
    }

    default List<A> getAll(S source) {
        return asFold().getAll(source);
    }

    default Maybe<Tuple2<I, A>> findWithIndex(BiPredicate<? super I, ? super A> predicate, S source) {
        return ifoldMap(
                Monoid.of(Maybe.none(), (left, right) -> left.isDefined() ? left : right),
                (index, value) -> predicate.test(index, value) ? Maybe.some(Tuple2.of(index, value)) : Maybe.none(),
                source);
    }

    default Maybe<Tuple2<I, A>> find(Predicate<? super A> predicate, S source) {
        return findWithIndex((index, value) -> predicate.test(value), source);
    }

    default int length(S source) {
        return ifoldMap(Monoid.of(0, Integer::sum), (index, value) -> 1, source);
    }

    default boolean isEmpty(S source) {
        return length(source) == 0;
    }

    default boolean existsWithIndex(BiPredicate<? super I, ? super A> predicate, S source) {
        return ifoldMap(Monoid.of(false, Boolean::logicalOr), predicate::test, source);
    }

    default boolean exists(Predicate<? super A> predicate, S source) {
        return existsWithIndex((index, value) -> predicate.test(value), source);
    }

    default boolean allWithIndex(BiPredicate<? super I, ? super A> predicate, S source) {
        return ifoldMap(Monoid.of(true, Boolean::logicalAnd), predicate::test, source);
    }

    default boolean all(Predicate<? super A> predicate, S source) {
        return allWithIndex((index, value) -> predicate.test(value), source);
    }

    default IndexedFold<I, S, A> filterIndex(Predicate<? super I> predicate) {
        IndexedFold<I, S, A> self = this;
        IndexedFold<I, S, A> direct = new IndexedFold<>() {
            @Override
            public <M> M ifoldMap(
                    Monoid<M> monoid, BiFunction<? super I, ? super A, ? extends M> f, S source) {
                return self.ifoldMap(
                        monoid,
                        (index, value) -> predicate.test(index) ? f.apply(index, value) : monoid.empty(),
                        source);
            }
        };
        return OpticPrograms.indexedFold(
                direct, OpticPrograms.structured("indexedFoldFilter", null));
    }

    default IndexedFold<I, S, A> filtered(Predicate<? super A> predicate) {
        IndexedFold<I, S, A> self = this;
        IndexedFold<I, S, A> direct = new IndexedFold<>() {
            @Override
            public <M> M ifoldMap(
                    Monoid<M> monoid, BiFunction<? super I, ? super A, ? extends M> f, S source) {
                return self.ifoldMap(
                        monoid,
                        (index, value) -> predicate.test(value) ? f.apply(index, value) : monoid.empty(),
                        source);
            }
        };
        return OpticPrograms.indexedFold(
                direct, OpticPrograms.structured("indexedFoldFiltered", null));
    }

    default <J, B> IndexedFold<Tuple2<I, J>, S, B> iandThen(IndexedFold<J, A, B> other) {
        IndexedFold<I, S, A> self = this;
        IndexedFold<Tuple2<I, J>, S, B> direct = new IndexedFold<>() {
            @Override
            public <M> M ifoldMap(
                    Monoid<M> monoid,
                    BiFunction<? super Tuple2<I, J>, ? super B, ? extends M> f,
                    S source) {
                return self.ifoldMap(
                        monoid,
                        (i, a) -> other.ifoldMap(monoid, (j, b) -> f.apply(Tuple2.of(i, j), b), a),
                        source);
            }
        };
        return OpticPrograms.indexedFold(direct, OpticPrograms.compose(this, other));
    }

    default <B> IndexedFold<I, S, B> andThen(Fold<A, B> other) {
        IndexedFold<I, S, A> self = this;
        IndexedFold<I, S, B> direct = new IndexedFold<>() {
            @Override
            public <M> M ifoldMap(
                    Monoid<M> monoid, BiFunction<? super I, ? super B, ? extends M> f, S source) {
                return self.ifoldMap(
                        monoid,
                        (index, value) -> other.foldMap(monoid, b -> f.apply(index, b), value),
                        source);
            }
        };
        return OpticPrograms.indexedFold(direct, OpticPrograms.compose(this, other));
    }

    default Fold<S, A> asFold() {
        IndexedFold<I, S, A> self = this;
        Fold<S, A> direct = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return self.ifoldMap(monoid, (index, value) -> f.apply(value), source);
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.programOrOpaque(this, "indexedFold"));
    }
}

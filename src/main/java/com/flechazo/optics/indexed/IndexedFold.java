package com.flechazo.optics.indexed;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.Fold;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a read-only optic that observes zero or more indexed focuses.
 *
 * @param <I> the index type
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface IndexedFold<I, S, A> {
    /**
     * Maps every index and focus to a monoid value and combines the results in encounter order.
     *
     * @param <M> the accumulated value type
     * @param monoid the identity and combination operation
     * @param f the function receiving each index and focus
     * @param source the source to observe
     * @return the combined value, or the monoid identity when no focus is present
     */
    <M> M ifoldMap(Monoid<M> monoid, BiFunction<? super I, ? super A, ? extends M> f, S source);

    /**
     * Returns all index-focus pairs in encounter order.
     *
     * @param source the source to observe
     * @return an unmodifiable list of index-focus tuples
     */
    default List<Tuple2<I, A>> toIndexedList(S source) {
        ArrayList<Tuple2<I, A>> values = new ArrayList<>();
        ifoldMap(
                Monoid.of(Unit.INSTANCE, (left, right) -> Unit.INSTANCE),
                (index, value) -> {
                    values.add(Tuple2.of(index, value));
                    return Unit.INSTANCE;
                },
                source);
        return Collections.unmodifiableList(values);
    }

    /**
     * Returns all focuses without their indexes.
     *
     * @param source the source to observe
     * @return an unmodifiable list of focuses
     */
    default List<A> getAll(S source) {
        return asFold().getAll(source);
    }

    /**
     * Returns the first index-focus pair satisfying an indexed predicate.
     *
     * @param predicate the condition receiving each index and focus
     * @param source the source to observe
     * @return the first matching pair, or an empty value
     */
    default Maybe<Tuple2<I, A>> findWithIndex(BiPredicate<? super I, ? super A> predicate, S source) {
        return ifoldMap(
                Monoid.of(Maybe.none(), (left, right) -> left.isDefined() ? left : right),
                (index, value) -> predicate.test(index, value) ? Maybe.some(Tuple2.of(index, value)) : Maybe.none(),
                source);
    }

    /**
     * Returns the first index-focus pair whose focus satisfies a predicate.
     *
     * @param predicate the condition applied to each focus
     * @param source the source to observe
     * @return the first matching pair, or an empty value
     */
    default Maybe<Tuple2<I, A>> find(Predicate<? super A> predicate, S source) {
        return findWithIndex((index, value) -> predicate.test(value), source);
    }

    /**
     * Returns the number of indexed focuses.
     *
     * @param source the source to observe
     * @return the focus count
     */
    default int length(S source) {
        return ifoldMap(Monoid.of(0, Integer::sum), (index, value) -> 1, source);
    }

    /**
     * Determines whether no indexed focus is present.
     *
     * @param source the source to observe
     * @return {@code true} when no focus is present
     */
    default boolean isEmpty(S source) {
        return length(source) == 0;
    }

    /**
     * Determines whether an index-focus pair satisfies a predicate.
     *
     * @param predicate the indexed condition
     * @param source the source to observe
     * @return {@code true} when a pair matches
     */
    default boolean existsWithIndex(BiPredicate<? super I, ? super A> predicate, S source) {
        return ifoldMap(Monoid.of(false, Boolean::logicalOr), predicate::test, source);
    }

    /**
     * Determines whether a focus satisfies a predicate.
     *
     * @param predicate the focus condition
     * @param source the source to observe
     * @return {@code true} when a focus matches
     */
    default boolean exists(Predicate<? super A> predicate, S source) {
        return existsWithIndex((index, value) -> predicate.test(value), source);
    }

    /**
     * Determines whether every index-focus pair satisfies a predicate.
     *
     * @param predicate the indexed condition
     * @param source the source to observe
     * @return {@code true} when every pair matches, including when no focus is present
     */
    default boolean allWithIndex(BiPredicate<? super I, ? super A> predicate, S source) {
        return ifoldMap(Monoid.of(true, Boolean::logicalAnd), predicate::test, source);
    }

    /**
     * Determines whether every focus satisfies a predicate.
     *
     * @param predicate the focus condition
     * @param source the source to observe
     * @return {@code true} when every focus matches, including when no focus is present
     */
    default boolean all(Predicate<? super A> predicate, S source) {
        return allWithIndex((index, value) -> predicate.test(value), source);
    }

    /**
     * Returns an indexed fold retaining indexes satisfying a predicate.
     *
     * @param predicate the index condition
     * @return the filtered indexed fold
     */
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

    /**
     * Returns an indexed fold retaining focuses satisfying a predicate.
     *
     * @param predicate the focus condition
     * @return the filtered indexed fold
     */
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

    /**
     * Composes this indexed fold with another indexed fold and pairs their indexes.
     *
     * @param <J> the nested index type
     * @param <B> the nested focus type
     * @param other the indexed fold applied to every focus
     * @return the composed indexed fold
     */
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

    /**
     * Composes this indexed fold with an unindexed fold while retaining the outer index.
     *
     * @param <B> the nested focus type
     * @param other the fold applied to every focus
     * @return the composed indexed fold
     */
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

    /**
     * Returns a fold that discards focus indexes.
     *
     * @return the unindexed fold
     */
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

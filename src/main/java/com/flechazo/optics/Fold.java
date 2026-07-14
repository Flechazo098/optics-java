package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Unit;
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

/**
 * Represents a read-only optic that observes zero or more focuses in encounter order.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface Fold<S, A> {
    /**
     * Maps every focus to a monoid value and combines the results in encounter order.
     *
     * @param <M> the accumulated value type
     * @param monoid the identity and combination operation
     * @param f the function applied to every focus
     * @param source the source to observe
     * @return the combined monoid value, or the monoid identity when no focus is present
     */
    <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source);

    /**
     * Returns all focuses in encounter order.
     *
     * @param source the source to observe
     * @return an unmodifiable list of all focuses
     */
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

    /**
     * Returns the first focus when one is present.
     *
     * @param source the source to observe
     * @return the first focus, or an empty value when the fold has no focus
     */
    default Maybe<A> preview(S source) {
        return foldMap(firstMaybeMonoid(), Maybe::some, source);
    }

    /**
     * Returns the first focus satisfying a predicate.
     *
     * @param predicate the condition applied in encounter order
     * @param source the source to observe
     * @return the first matching focus, or an empty value when no focus matches
     */
    default Maybe<A> find(Predicate<? super A> predicate, S source) {
        return foldMap(
                firstMaybeMonoid(),
                value -> predicate.test(value) ? Maybe.some(value) : Maybe.none(),
                source);
    }

    /**
     * Returns the first focus or a supplied default value.
     *
     * @param defaultValue the value returned when no focus is present
     * @param source the source to observe
     * @return the first focus, or {@code defaultValue} when no focus is present
     */
    default A firstOrElse(A defaultValue, S source) {
        return preview(source).orElse(defaultValue);
    }

    /**
     * Returns the first focus or obtains a default value lazily.
     *
     * @param defaultValue the supplier evaluated only when no focus is present
     * @param source the source to observe
     * @return the first focus, or the supplied value when no focus is present
     */
    default A firstOrElseGet(Supplier<? extends A> defaultValue, S source) {
        return preview(source).orElseGet(defaultValue);
    }

    /**
     * Returns the number of focuses observed from a source.
     *
     * @param source the source to observe
     * @return the number of focuses
     */
    default int length(S source) {
        return foldMap(Monoid.of(0, Integer::sum), ignored -> 1, source);
    }

    /**
     * Determines whether a source contains no focus.
     *
     * @param source the source to observe
     * @return {@code true} when no focus is present
     */
    default boolean isEmpty(S source) {
        return length(source) == 0;
    }

    /**
     * Determines whether at least one focus satisfies a predicate.
     *
     * @param predicate the condition applied to focuses
     * @param source the source to observe
     * @return {@code true} when at least one focus satisfies the predicate
     */
    default boolean exists(Predicate<? super A> predicate, S source) {
        return foldMap(Monoid.of(false, Boolean::logicalOr), predicate::test, source);
    }

    /**
     * Determines whether every focus satisfies a predicate.
     *
     * @param predicate the condition applied to focuses
     * @param source the source to observe
     * @return {@code true} when every focus satisfies the predicate, including when no focus is
     * present
     */
    default boolean all(Predicate<? super A> predicate, S source) {
        return foldMap(Monoid.of(true, Boolean::logicalAnd), predicate::test, source);
    }

    /**
     * Performs two monoidal folds in one traversal and combines their results.
     *
     * @param <M> the first accumulated value type
     * @param <N> the second accumulated value type
     * @param <R> the combined result type
     * @param firstMonoid the first identity and combination operation
     * @param first the first mapping function
     * @param secondMonoid the second identity and combination operation
     * @param second the second mapping function
     * @param combineResult the function combining both accumulated values
     * @param source the source to observe
     * @return the combined fold result
     */
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

    /**
     * Composes this fold with another fold.
     *
     * @param <B> the composed focus type
     * @param other the fold applied to every focus
     * @return the composed fold
     */
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

    /**
     * Composes this fold with a monomorphic prism.
     *
     * @param <B> the composed focus type
     * @param prism the prism applied to every focus
     * @return the composed fold
     */
    default <B> Fold<S, B> andThen(PPrism<A, A, B, B> prism) {
        return andThen(prism.asFold());
    }

    /**
     * Composes this fold with a monomorphic lens.
     *
     * @param <B> the composed focus type
     * @param lens the lens applied to every focus
     * @return the composed fold
     */
    default <B> Fold<S, B> andThen(PLens<A, A, B, B> lens) {
        return andThen(lens.asFold());
    }

    /**
     * Composes this fold with a monomorphic affine optic.
     *
     * @param <B> the composed focus type
     * @param affine the affine optic applied to every focus
     * @return the composed fold
     */
    default <B> Fold<S, B> andThen(PAffine<A, A, B, B> affine) {
        return andThen(affine.asFold());
    }

    /**
     * Composes this fold with a monomorphic traversal.
     *
     * @param <B> the composed focus type
     * @param traversal the traversal applied to every focus
     * @return the composed fold
     */
    default <B> Fold<S, B> andThen(PTraversal<A, A, B, B> traversal) {
        return andThen(traversal.asFold());
    }

    /**
     * Returns a fold that observes only the focus at an encounter index.
     *
     * @param index the zero-based focus index
     * @return a fold containing the indexed focus, or no focus when the index is negative or out
     * of range
     */
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

    /**
     * Returns a fold that retains focuses satisfying a predicate.
     *
     * @param predicate the condition used to retain focuses
     * @return the filtered fold
     */
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

    /**
     * Returns a fold that retains a focus when a nested query has a matching result.
     *
     * @param <B> the nested query focus type
     * @param query the fold evaluated against each focus
     * @param predicate the condition applied to nested query results
     * @return a fold retaining focuses whose nested query has at least one match
     */
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

    /**
     * Returns a fold that observes this fold's focuses followed by another fold's focuses.
     *
     * @param other the fold appended after this fold
     * @return the combined fold
     */
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

    /**
     * Returns a fold that never observes a focus.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @return the empty fold
     */
    static <S, A> Fold<S, A> empty() {
        Fold<S, A> direct = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return monoid.empty();
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.structured("emptyFold", null));
    }

    /**
     * Creates a fold from a function that enumerates focuses.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getAll the function returning focuses in encounter order
     * @return the resulting fold
     */
    static <S, A> Fold<S, A> of(Function<? super S, ? extends Iterable<? extends A>> getAll) {
        Fold<S, A> direct = iterableFold(getAll);
        return OpticPrograms.fold(direct, OpticPrograms.opaque("fold", null));
    }

    /**
     * Creates a fold from a serializable function that enumerates focuses.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getAll the serializable function returning focuses in encounter order
     * @return the resulting fold
     */
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

    /**
     * Returns a fold over map keys in key iteration order.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @return the map-key fold
     */
    static <K, V> Fold<Map<K, V>, K> mapKeys() {
        return Fold.of(Map::keySet);
    }

    /**
     * Returns a fold over map values in value iteration order.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @return the map-value fold
     */
    static <K, V> Fold<Map<K, V>, V> mapValues() {
        return Fold.of(Map::values);
    }

    /**
     * Returns a fold over map entries in entry iteration order.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @return the map-entry fold
     */
    static <K, V> Fold<Map<K, V>, Map.Entry<K, V>> mapEntries() {
        return Fold.of(Map::entrySet);
    }

    /**
     * Returns a fold that concatenates the focuses of all supplied folds.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param first the first fold
     * @param rest the folds appended after {@code first}
     * @return the combined fold
     */
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

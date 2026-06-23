package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.optics.util.Optionals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Fold<S, A> extends Optic<S, S, A, A> {
    <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source);

    @Override
    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
        Monoid<App<F, Unit>> effects =
                Monoid.of(
                        applicative.of(Unit.INSTANCE),
                        (left, right) -> applicative.map2(left, right, (a, b) -> Unit.INSTANCE));
        App<F, Unit> sequenced =
                foldMap(effects, value -> applicative.map(ignored -> Unit.INSTANCE, f.apply(value)), source);
        return applicative.map(ignored -> source, sequenced);
    }

    default List<A> getAll(S source) {
        ArrayList<A> values = new ArrayList<>();
        foldMap(
                Monoid.of(Unit.INSTANCE, (left, right) -> Unit.INSTANCE),
                value -> {
                    values.add(value);
                    return Unit.INSTANCE;
                },
                source);
        return values;
    }

    default Maybe<A> preview(S source) {
        return foldMap(firstMaybeMonoid(), Maybe::some, source);
    }

    default Optional<A> previewOptional(S source) {
        return Optionals.fromMaybe(preview(source));
    }

    default Maybe<A> find(Predicate<? super A> predicate, S source) {
        return foldMap(
                firstMaybeMonoid(),
                value -> predicate.test(value) ? Maybe.some(value) : Maybe.none(),
                source);
    }

    default Optional<A> findOptional(Predicate<? super A> predicate, S source) {
        return Optionals.fromMaybe(find(predicate, source));
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
        Pair<M, N> folded =
                foldMap(
                        Monoid.product(firstMonoid, secondMonoid),
                        value -> Pair.of(first.apply(value), second.apply(value)),
                        source);
        return combineResult.apply(folded.first(), folded.second());
    }

    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        Fold<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super B, ? extends M> f, S source) {
                return self.foldMap(monoid, value -> other.foldMap(monoid, f, value), source);
            }
        };
    }

    default <B> Fold<S, B> andThen(Prism<A, B> prism) {
        return andThen(prism.asFold());
    }

    default <B> Fold<S, B> andThen(Lens<A, B> lens) {
        return andThen(lens.asFold());
    }

    default <B> Fold<S, B> andThen(Affine<A, B> affine) {
        return andThen(affine.asFold());
    }

    default <B> Fold<S, B> andThen(Traversal<A, B> traversal) {
        return andThen(traversal.asFold());
    }

    default Affine<S, A> at(int index) {
        Fold<S, A> self = this;
        return new Affine<>() {
            @Override
            public Maybe<A> getMaybe(S source) {
                if (index < 0) {
                    return Maybe.none();
                }
                int current = 0;
                for (A value : self.getAll(source)) {
                    if (current == index) {
                        return Maybe.some(value);
                    }
                    current++;
                }
                return Maybe.none();
            }

            @Override
            public S set(A value, S source) {
                if (getMaybe(source).isEmpty()) {
                    return source;
                }
                throw new UnsupportedOperationException("Cannot set through a Fold");
            }
        };
    }

    default Fold<S, A> filtered(Predicate<? super A> predicate) {
        Fold<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return self.foldMap(monoid, value -> predicate.test(value) ? f.apply(value) : monoid.empty(), source);
            }
        };
    }

    default <B> Fold<S, A> filterBy(Fold<A, B> query, Predicate<? super B> predicate) {
        Fold<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return self.foldMap(
                        monoid,
                        value -> query.exists(predicate, value) ? f.apply(value) : monoid.empty(),
                        source);
            }
        };
    }

    default Fold<S, A> plus(Fold<S, A> other) {
        Fold<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return monoid.combine(self.foldMap(monoid, f, source), other.foldMap(monoid, f, source));
            }
        };
    }

    static <S, A> Fold<S, A> empty() {
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return monoid.empty();
            }
        };
    }

    static <S, A> Fold<S, A> of(Function<? super S, ? extends Iterable<? extends A>> getAll) {
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                M result = monoid.empty();
                for (A value : getAll.apply(source)) {
                    result = monoid.combine(result, f.apply(value));
                }
                return result;
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

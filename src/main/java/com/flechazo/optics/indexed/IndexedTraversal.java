package com.flechazo.optics.indexed;

import com.flechazo.hkt.*;
import com.flechazo.optics.Lens;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface IndexedTraversal<I, S, A> extends IndexedOptic<I, S, A> {
    @Override
    <F extends K1> App<F, S> imodifyF(
            BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative);

    default S imodify(BiFunction<? super I, ? super A, ? extends A> f, S source) {
        App<IdF.Mu, S> result =
                imodifyF((index, value) -> new IdF<>(f.apply(index, value)), source, IdF.applicative());
        return IdF.get(result);
    }

    default Traversal<S, A> asTraversal() {
        IndexedTraversal<I, S, A> self = this;
        Traversal<S, A> direct = new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF((index, value) -> f.apply(value), source, applicative);
            }
        };
        return OpticPrograms.traversal(
                direct, OpticPrograms.programOrOpaque(this, "indexedTraversal"));
    }

    default IndexedFold<I, S, A> asIndexedFold() {
        IndexedTraversal<I, S, A> self = this;
        IndexedFold<I, S, A> direct = new IndexedFold<>() {
            @Override
            public <M> M ifoldMap(
                    Monoid<M> monoid,
                    BiFunction<? super I, ? super A, ? extends M> f,
                    S source) {
                final ArrayList<Tuple2<I, A>> pairs = new ArrayList<>();
                self.imodifyF(
                        (index, value) -> {
                            pairs.add(Tuple2.of(index, value));
                            return new IdF<>(value);
                        },
                        source,
                        IdF.applicative());
                M result = monoid.empty();
                for (Tuple2<I, A> Tuple2 : pairs) {
                    result = monoid.combine(result, f.apply(Tuple2.first(), Tuple2.second()));
                }
                return result;
            }
        };
        return OpticPrograms.indexedFold(
                direct, OpticPrograms.programOrOpaque(this, "indexedTraversal"));
    }

    default <J, B> IndexedTraversal<Tuple2<I, J>, S, B> iandThen(
            IndexedTraversal<J, A, B> other) {
        IndexedTraversal<I, S, A> self = this;
        IndexedTraversal<Tuple2<I, J>, S, B> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<Tuple2<I, J>, B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (i, a) -> other.imodifyF((j, b) -> f.apply(Tuple2.of(i, j), b), a, applicative),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.indexedTraversal(direct, OpticPrograms.compose(this, other));
    }

    default <B> IndexedTraversal<I, S, B> andThen(Traversal<A, B> other) {
        IndexedTraversal<I, S, A> self = this;
        IndexedTraversal<I, S, B> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<I, B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (index, value) -> other.modifyF(next -> f.apply(index, next), value, applicative),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.indexedTraversal(direct, OpticPrograms.compose(this, other));
    }

    default <B> IndexedTraversal<I, S, B> andThen(Lens<A, B> other) {
        return andThen(other.asTraversal());
    }

    default IndexedTraversal<I, S, A> filterIndex(Predicate<? super I> predicate) {
        IndexedTraversal<I, S, A> self = this;
        IndexedTraversal<I, S, A> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (index, value) -> predicate.test(index) ? f.apply(index, value) : applicative.of(value),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.structured("indexedFilter", null));
    }

    default IndexedTraversal<I, S, A> filtered(Predicate<? super A> predicate) {
        IndexedTraversal<I, S, A> self = this;
        IndexedTraversal<I, S, A> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (index, value) -> predicate.test(value) ? f.apply(index, value) : applicative.of(value),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.structured("indexedFiltered", null));
    }

    default IndexedTraversal<I, S, A> filteredWithIndex(
            BiFunction<? super I, ? super A, Boolean> predicate) {
        IndexedTraversal<I, S, A> self = this;
        IndexedTraversal<I, S, A> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (index, value) ->
                                Boolean.TRUE.equals(predicate.apply(index, value))
                                        ? f.apply(index, value)
                                        : applicative.of(value),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.structured("indexedFilteredWithIndex", null));
    }

    static <A> IndexedTraversal<Integer, List<A>, A> forList() {
        IndexedTraversal<Integer, List<A>, A> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, List<A>> imodifyF(
                    BiFunction<Integer, A, App<F, A>> f, List<A> source, Applicative<F, ?> applicative) {
                App<F, List<A>> acc = applicative.of(new ArrayList<>(source.size()));
                for (int i = 0; i < source.size(); i++) {
                    final int index = i;
                    acc =
                            applicative.map2(
                                    acc,
                                    f.apply(index, source.get(index)),
                                    (list, value) -> {
                                        ArrayList<A> next = new ArrayList<>(list);
                                        next.add(value);
                                        return next;
                                    });
                }
                return acc;
            }
        };
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.structured("indexedListTraversal", Integer.class));
    }

    static <K, V> IndexedTraversal<K, Map<K, V>, V> forMapValues() {
        IndexedTraversal<K, Map<K, V>, V> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, Map<K, V>> imodifyF(
                    BiFunction<K, V, App<F, V>> f, Map<K, V> source, Applicative<F, ?> applicative) {
                App<F, LinkedHashMap<K, V>> acc = applicative.of(new LinkedHashMap<>());
                for (Map.Entry<K, V> entry : source.entrySet()) {
                    K key = entry.getKey();
                    acc =
                            applicative.map2(
                                    acc,
                                    f.apply(key, entry.getValue()),
                                    (map, value) -> {
                                        LinkedHashMap<K, V> next = new LinkedHashMap<>(map);
                                        next.put(key, value);
                                        return next;
                                    });
                }
                return applicative.map(map -> map, acc);
            }
        };
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.structured("indexedMapValuesTraversal", null));
    }
}

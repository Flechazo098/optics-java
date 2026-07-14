package com.flechazo.optics.indexed;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.data.Chain;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.Lens;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a traversal that supplies an index with every focus.
 *
 * @param <I> the index type
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface IndexedTraversal<I, S, A> extends IndexedOptic<I, S, A> {
    /**
     * Applies an indexed applicative transformation to every focus.
     *
     * @param <F> the applicative witness type
     * @param f the effectful indexed transformation
     * @param source the source to transform
     * @param applicative the applicative used to combine effects and rebuild the source
     * @return the rebuilt source in the applicative context
     */
    @Override
    <F extends K1> App<F, S> imodifyF(
            BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative);

    /**
     * Transforms every focus using its index.
     *
     * @param f the indexed focus transformation
     * @param source the source to transform
     * @return the rebuilt source
     */
    default S imodify(BiFunction<? super I, ? super A, ? extends A> f, S source) {
        App<IdF.Mu, S> result =
                imodifyF((index, value) -> new IdF<>(f.apply(index, value)), source, IdF.applicative());
        return IdF.get(result);
    }

    /**
     * Applies an indexed effectful modifier when an indexed effectful condition is true.
     *
     * @param <F> the selective witness type
     * @param condition the indexed effectful condition
     * @param modifier the indexed effectful transformation
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, S> imodifyWhenS(
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.imodifyWhen(this, condition, modifier, source, selective);
    }

    /**
     * Applies an indexed effectful modifier when an indexed effectful condition is false.
     *
     * @param <F> the selective witness type
     * @param condition the indexed effectful condition
     * @param modifier the indexed effectful transformation
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, S> imodifyUnlessS(
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.imodifyUnless(this, condition, modifier, source, selective);
    }

    /**
     * Returns a traversal that discards focus indexes.
     *
     * @return the unindexed traversal
     */
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

    /**
     * Returns an indexed fold observing the same indexes and focuses.
     *
     * @return the indexed fold view
     */
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

    /**
     * Composes this indexed traversal with another indexed traversal and pairs their indexes.
     *
     * @param <J> the nested index type
     * @param <B> the nested focus type
     * @param other the indexed traversal applied to every focus
     * @return the composed indexed traversal
     */
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

    /**
     * Composes this indexed traversal with an unindexed traversal while retaining the outer index.
     *
     * @param <B> the nested focus type
     * @param other the traversal applied to every focus
     * @return the composed indexed traversal
     */
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

    /**
     * Composes this indexed traversal with a lens while retaining the outer index.
     *
     * @param <B> the nested focus type
     * @param other the lens applied to every focus
     * @return the composed indexed traversal
     */
    default <B> IndexedTraversal<I, S, B> andThen(Lens<A, B> other) {
        return andThen(other.asTraversal());
    }

    /**
     * Returns an indexed traversal retaining indexes satisfying a predicate.
     *
     * @param predicate the index condition
     * @return the filtered indexed traversal
     */
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

    /**
     * Returns an indexed traversal retaining focuses satisfying a predicate.
     *
     * @param predicate the focus condition
     * @return the filtered indexed traversal
     */
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

    /**
     * Returns an indexed traversal retaining index-focus pairs satisfying a predicate.
     *
     * @param predicate the condition receiving each index and focus
     * @return the filtered indexed traversal
     */
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

    /**
     * Returns an indexed traversal over list elements in ascending index order.
     *
     * @param <A> the element type
     * @return the indexed list traversal
     */
    static <A> IndexedTraversal<Integer, List<A>, A> forList() {
        IndexedTraversal<Integer, List<A>, A> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, List<A>> imodifyF(
                    BiFunction<Integer, A, App<F, A>> f, List<A> source, Applicative<F, ?> applicative) {
                App<F, Chain<A>> acc = applicative.of(Chain.empty());
                for (int i = 0; i < source.size(); i++) {
                    final int index = i;
                    acc = applicative.map2(
                            acc,
                            f.apply(index, source.get(index)),
                            Chain::append);
                }
                return applicative.map(Chain::toList, acc);
            }
        };
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.structured("indexedListTraversal", Integer.class));
    }

    /**
     * Returns an indexed traversal over map values using their keys as indexes.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the indexed map-value traversal
     */
    static <K, V> IndexedTraversal<K, Map<K, V>, V> forMapValues() {
        IndexedTraversal<K, Map<K, V>, V> direct = new IndexedTraversal<>() {
            @Override
            public <F extends K1> App<F, Map<K, V>> imodifyF(
                    BiFunction<K, V, App<F, V>> f, Map<K, V> source, Applicative<F, ?> applicative) {
                App<F, Chain<Tuple2<K, V>>> acc = applicative.of(Chain.empty());
                for (Map.Entry<K, V> entry : source.entrySet()) {
                    K key = entry.getKey();
                    acc = applicative.map2(
                            acc,
                            f.apply(key, entry.getValue()),
                            (values, value) -> values.prepend(Tuple2.of(key, value)));
                }
                return applicative.map(values -> {
                    LinkedHashMap<K, V> result = new LinkedHashMap<>(source.size());
                    for (Tuple2<K, V> entry : values.toList()) {
                        result.put(entry.first(), entry.second());
                    }
                    return Collections.unmodifiableMap(result);
                }, acc);
            }
        };
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.structured("indexedMapValuesTraversal", null));
    }
}

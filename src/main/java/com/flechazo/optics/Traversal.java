package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeOptic;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Traversal<S, T, A, B> extends Optic<S, T, A, B> {
    @Override
    <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative);

    default T modify(Function<? super A, ? extends B> f, S source) {
        App<IdF.Mu, T> result =
                modifyF(value -> new IdF<>(f.apply(value)), source, IdF.applicative());
        return IdF.get(result);
    }

    default T set(B value, S source) {
        return modify(ignored -> value, source);
    }

    default List<A> getAll(S source) {
        return asFold().getAll(source);
    }

    default Maybe<A> preview(S source) {
        return asFold().preview(source);
    }

    default int length(S source) {
        return asFold().length(source);
    }

    default boolean exists(Predicate<? super A> predicate, S source) {
        return asFold().exists(predicate, source);
    }

    default boolean all(Predicate<? super A> predicate, S source) {
        return asFold().all(predicate, source);
    }

    default Fold<S, A> asFold() {
        Traversal<S, T, A, B> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Applicative<Const.Mu<M>, ?> app = Const.applicative(monoid);
                App<Const.Mu<M>, T> folded =
                        self.modifyF(value -> new Const<>(f.apply(value)), source, app);
                return Const.get(folded);
            }
        };
    }

    default Setter<S, T, A, B> asSetter() {
        Traversal<S, T, A, B> self = this;
        return new Setter<>() {
            @Override
            public T modify(Function<? super A, ? extends B> f, S source) {
                return self.modify(f, source);
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(f, source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<S, T, A, B>> typedOptic() {
                return self.typedOptic();
            }
        };
    }

    default <C, D> Traversal<S, T, C, D> andThen(Traversal<A, B, C, D> other) {
        Traversal<S, T, A, B> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<S, T, C, D>> typedOptic() {
                return self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen));
            }
        };
    }

    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        return asFold().andThen(fold);
    }

    default <C, D> Traversal<S, T, C, D> andThen(Lens<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    default <C, D> Traversal<S, T, C, D> andThen(Prism<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    default <C, D> Traversal<S, T, C, D> andThen(Affine<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    default Traversal<S, T, A, B> filtered(Predicate<? super A> predicate) {
        Traversal<S, T, A, B> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(
                        value -> predicate.test(value) ? f.apply(value) : applicative.of(cast(value)),
                        source,
                        applicative);
            }
        };
    }

    default T modifyWhen(Predicate<? super A> predicate, Function<? super A, ? extends B> f, S source) {
        return filtered(predicate).modify(f, source);
    }

    static <K, V> Traversal<Map<K, V>, Map<K, V>, V, V> mapValues() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Map<K, V>> modifyF(
                    Function<V, App<F, V>> f, Map<K, V> source, Applicative<F, ?> applicative) {
                App<F, LinkedHashMap<K, V>> acc = applicative.of(new LinkedHashMap<>());
                for (Map.Entry<K, V> entry : source.entrySet()) {
                    K key = entry.getKey();
                    acc =
                            applicative.map2(
                                    acc,
                                    f.apply(entry.getValue()),
                                    (map, next) -> {
                                        LinkedHashMap<K, V> copy = new LinkedHashMap<>(map);
                                        copy.put(key, next);
                                        return copy;
                                    });
                }
                return applicative.map(map -> map, acc);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <B> B cast(Object value) {
        return (B) value;
    }
}

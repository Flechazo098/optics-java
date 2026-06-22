package com.flechazo.optics;

import com.flechazo.hkt.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Traversal<S, A> extends Optic<S, S, A, A> {
    @Override
    <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F> applicative);

    default S modify(Function<? super A, ? extends A> f, S source) {
        App<IdF.Mu, S> result =
                modifyF(value -> new IdF<>(f.apply(value)), source, IdF.applicative());
        return IdF.narrow(result).value();
    }

    default S set(A value, S source) {
        return modify(ignored -> value, source);
    }

    default java.util.List<A> getAll(S source) {
        return asFold().getAll(source);
    }

    default Maybe<A> preview(S source) {
        return asFold().preview(source);
    }

    default Affine<S, A> at(int index) {
        Traversal<S, A> self = this;
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
                if (index < 0 || getMaybe(source).isEmpty()) {
                    return source;
                }
                int[] current = {0};
                return self.modify(target -> current[0]++ == index ? value : target, source);
            }
        };
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
        Traversal<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Applicative<Const.Mu<M>> app = Const.applicative(monoid);
                App<Const.Mu<M>, S> folded =
                        self.modifyF(value -> new Const<>(f.apply(value)), source, app);
                return Const.narrow(folded).value();
            }
        };
    }

    default Setter<S, A> asSetter() {
        Traversal<S, A> self = this;
        return new Setter<>() {
            @Override
            public S modify(Function<? super A, ? extends A> f, S source) {
                return self.modify(f, source);
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
                return self.modifyF(f, source, applicative);
            }
        };
    }

    default <B> Traversal<S, B> andThen(Traversal<A, B> other) {
        Traversal<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }

            @Override
            public S modify(Function<? super B, ? extends B> f, S source) {
                return self.modify(value -> other.modify(f, value), source);
            }

            @Override
            public List<B> getAll(S source) {
                ArrayList<B> result = new ArrayList<>();
                for (A value : self.getAll(source)) {
                    result.addAll(other.getAll(value));
                }
                return List.copyOf(result);
            }

            @Override
            public Maybe<B> preview(S source) {
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.preview(value);
                    if (found.isDefined()) {
                        return found;
                    }
                }
                return Maybe.none();
            }

            @Override
            public int length(S source) {
                int count = 0;
                for (A value : self.getAll(source)) {
                    count += other.length(value);
                }
                return count;
            }

            @Override
            public boolean exists(Predicate<? super B> predicate, S source) {
                for (A value : self.getAll(source)) {
                    if (other.exists(predicate, value)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean all(Predicate<? super B> predicate, S source) {
                for (A value : self.getAll(source)) {
                    if (!other.all(predicate, value)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    default <B> Fold<S, B> andThen(Fold<A, B> fold) {
        return asFold().andThen(fold);
    }

    default <B> Traversal<S, B> andThen(Lens<A, B> other) {
        Traversal<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }

            @Override
            public S modify(Function<? super B, ? extends B> f, S source) {
                return self.modify(value -> other.modify(f, value), source);
            }

            @Override
            public List<B> getAll(S source) {
                ArrayList<B> result = new ArrayList<>();
                for (A value : self.getAll(source)) {
                    result.add(other.get(value));
                }
                return List.copyOf(result);
            }

            @Override
            public Maybe<B> preview(S source) {
                Maybe<A> found = self.preview(source);
                return found.map(other::get);
            }

            @Override
            public int length(S source) {
                return self.length(source);
            }

            @Override
            public boolean exists(Predicate<? super B> predicate, S source) {
                for (A value : self.getAll(source)) {
                    if (predicate.test(other.get(value))) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean all(Predicate<? super B> predicate, S source) {
                for (A value : self.getAll(source)) {
                    if (!predicate.test(other.get(value))) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    default <B> Traversal<S, B> andThen(Prism<A, B> other) {
        Traversal<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }

            @Override
            public S modify(Function<? super B, ? extends B> f, S source) {
                return self.modify(value -> other.modify(f, value), source);
            }

            @Override
            public List<B> getAll(S source) {
                ArrayList<B> result = new ArrayList<>();
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.getMaybe(value);
                    if (found.isDefined()) {
                        result.add(found.get());
                    }
                }
                return List.copyOf(result);
            }

            @Override
            public Maybe<B> preview(S source) {
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.getMaybe(value);
                    if (found.isDefined()) {
                        return found;
                    }
                }
                return Maybe.none();
            }

            @Override
            public int length(S source) {
                int count = 0;
                for (A value : self.getAll(source)) {
                    if (other.matches(value)) {
                        count++;
                    }
                }
                return count;
            }

            @Override
            public boolean exists(Predicate<? super B> predicate, S source) {
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.getMaybe(value);
                    if (found.isDefined() && predicate.test(found.get())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean all(Predicate<? super B> predicate, S source) {
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.getMaybe(value);
                    if (found.isDefined() && !predicate.test(found.get())) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    default <B> Traversal<S, B> andThen(Affine<A, B> other) {
        Traversal<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }

            @Override
            public S modify(Function<? super B, ? extends B> f, S source) {
                return self.modify(value -> other.modify(f, value), source);
            }

            @Override
            public List<B> getAll(S source) {
                ArrayList<B> result = new ArrayList<>();
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.getMaybe(value);
                    if (found.isDefined()) {
                        result.add(found.get());
                    }
                }
                return List.copyOf(result);
            }

            @Override
            public Maybe<B> preview(S source) {
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.getMaybe(value);
                    if (found.isDefined()) {
                        return found;
                    }
                }
                return Maybe.none();
            }

            @Override
            public int length(S source) {
                int count = 0;
                for (A value : self.getAll(source)) {
                    if (other.matches(value)) {
                        count++;
                    }
                }
                return count;
            }

            @Override
            public boolean exists(Predicate<? super B> predicate, S source) {
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.getMaybe(value);
                    if (found.isDefined() && predicate.test(found.get())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean all(Predicate<? super B> predicate, S source) {
                for (A value : self.getAll(source)) {
                    Maybe<B> found = other.getMaybe(value);
                    if (found.isDefined() && !predicate.test(found.get())) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    default Traversal<S, A> filtered(Predicate<? super A> predicate) {
        Traversal<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
                return self.modifyF(
                        value -> predicate.test(value) ? f.apply(value) : applicative.of(value),
                        source,
                        applicative);
            }
        };
    }

    default <B> Traversal<S, A> filterBy(Fold<A, B> query, Predicate<? super B> predicate) {
        Traversal<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
                return self.modifyF(
                        value -> query.exists(predicate, value) ? f.apply(value) : applicative.of(value),
                        source,
                        applicative);
            }
        };
    }

    default S modifyWhen(Predicate<? super A> predicate, Function<? super A, ? extends A> f, S source) {
        return filtered(predicate).modify(f, source);
    }

    default S modifyBranch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends A> thenModifier,
            Function<? super A, ? extends A> elseModifier,
            S source) {
        return modify(value -> predicate.test(value) ? thenModifier.apply(value) : elseModifier.apply(value), source);
    }

    default <F extends K1> App<F, S> modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> f,
            S source,
            Selective<F> selective) {
        Objects.requireNonNull(selective, "selective");
        return modifyF(
                value -> selective.ifS(
                        selective.of(predicate.test(value)),
                        () -> Objects.requireNonNull(f.apply(value), "modify result"),
                        () -> selective.of(value)),
                source,
                selective);
    }

    default <F extends K1> App<F, S> branch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> thenBranch,
            Function<? super A, ? extends App<F, A>> elseBranch,
            S source,
            Selective<F> selective) {
        return modifyBranch(predicate, thenBranch, elseBranch, source, selective);
    }

    default <F extends K1> App<F, S> modifyBranch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> thenModifier,
            Function<? super A, ? extends App<F, A>> elseModifier,
            S source,
            Selective<F> selective) {
        Objects.requireNonNull(selective, "selective");
        return modifyF(
                value -> selective.ifS(
                        selective.of(predicate.test(value)),
                        () -> Objects.requireNonNull(thenModifier.apply(value), "then modifier result"),
                        () -> Objects.requireNonNull(elseModifier.apply(value), "else modifier result")),
                source,
                selective);
    }

    static <K, V> Traversal<Map<K, V>, V> mapValues() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Map<K, V>> modifyF(
                    Function<V, App<F, V>> f, Map<K, V> source, Applicative<F> applicative) {
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

    static <K, V> Traversal<Map<K, V>, Pair<K, V>> mapEntries() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Map<K, V>> modifyF(
                    Function<Pair<K, V>, App<F, Pair<K, V>>> f, Map<K, V> source, Applicative<F> applicative) {
                App<F, LinkedHashMap<K, V>> acc = applicative.of(new LinkedHashMap<>());
                for (Map.Entry<K, V> entry : source.entrySet()) {
                    acc =
                            applicative.map2(
                                    acc,
                                    f.apply(Pair.of(entry.getKey(), entry.getValue())),
                                    (map, next) -> {
                                        LinkedHashMap<K, V> copy = new LinkedHashMap<>(map);
                                        copy.put(next.first(), next.second());
                                        return copy;
                                    });
                }
                return applicative.map(map -> map, acc);
            }
        };
    }
}

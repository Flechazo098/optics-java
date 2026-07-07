package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.generated.GeneratedTraversal;
import com.google.common.reflect.TypeToken;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Traversals {
    private Traversals() {
    }

    public static <S, A> Optional<A> previewOptional(Traversal<S, S, A, A> traversal, S source) {
        return Optionals.fromMaybe(traversal.preview(source));
    }

    public static <S, A> Optional<A> findOptional(
            Traversal<S, S, A, A> traversal, Predicate<? super A> predicate, S source) {
        return Optionals.fromMaybe(traversal.asFold().find(predicate, source));
    }

    public static <A> Traversal<A, A, A, A> filtered(Predicate<? super A> predicate) {
        return new Traversal<>() {
            @Override
            public <F extends K1>
            App<F, A> modifyF(
                    Function<A, App<F, A>> f,
                    A source,
                    Applicative<F, ?> applicative) {
                return predicate.test(source) ? f.apply(source) : applicative.of(source);
            }
        };
    }

    public static <A> Traversal<List<A>, List<A>, A, A> forList() {
        return new GeneratedTraversal<>(GeneratedTraversal.LIST, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }

    public static <A> Traversal<List<A>, List<A>, A, A> forList(TypeToken<A> elementType) {
        return forList(Types.witness(elementType));
    }

    public static <A> Traversal<List<A>, List<A>, A, A> forList(Type<A> elementType) {
        Traversal<List<A>, List<A>, A, A> executable = forList();
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, List<A>> modifyF(
                    Function<A, App<F, A>> f,
                    List<A> source,
                    Applicative<F, ?> applicative) {
                return executable.modifyF(f, source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<List<A>, List<A>, A, A>> typedOptic() {
                return Maybe.some(PointFreeOptic.list(elementType));
            }
        };
    }

    public static <A> Traversal<Maybe<A>, Maybe<A>, A, A> forMaybe() {
        return new GeneratedTraversal<>(GeneratedTraversal.MAYBE, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }

    public static <A> Traversal<Maybe<A>, Maybe<A>, A, A> forMaybe(TypeToken<A> elementType) {
        return forMaybe(Types.witness(elementType));
    }

    public static <A> Traversal<Maybe<A>, Maybe<A>, A, A> forMaybe(Type<A> elementType) {
        Traversal<Maybe<A>, Maybe<A>, A, A> executable = forMaybe();
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Maybe<A>> modifyF(
                    Function<A, App<F, A>> f,
                    Maybe<A> source,
                    Applicative<F, ?> applicative) {
                return executable.modifyF(f, source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<Maybe<A>, Maybe<A>, A, A>> typedOptic() {
                return Maybe.some(PointFreeOptic.maybe(elementType));
            }
        };
    }

    public static <K, V> Traversal<Map<K, V>, Map<K, V>, V, V> forMapValues() {
        return new GeneratedTraversal<>(GeneratedTraversal.MAP_VALUES, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }

    public static <K, V> Traversal<Map<K, V>, Map<K, V>, V, V> forMapValues(
            TypeToken<K> keyType,
            TypeToken<V> valueType) {
        return forMapValues(Types.witness(keyType), Types.witness(valueType));
    }

    public static <K, V> Traversal<Map<K, V>, Map<K, V>, V, V> forMapValues(
            Type<K> keyType,
            Type<V> valueType) {
        Traversal<Map<K, V>, Map<K, V>, V, V> executable = forMapValues();
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Map<K, V>> modifyF(
                    Function<V, App<F, V>> f,
                    Map<K, V> source,
                    Applicative<F, ?> applicative) {
                return executable.modifyF(f, source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<Map<K, V>, Map<K, V>, V, V>> typedOptic() {
                return Maybe.some(PointFreeOptic.mapValues(keyType, valueType));
            }
        };
    }

    public static <K, V> Traversal<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> forMapEntries(
            TypeToken<K> keyType,
            TypeToken<V> valueType) {
        return forMapEntries(Types.witness(keyType), Types.witness(valueType));
    }

    public static <K, V> Traversal<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> forMapEntries(
            Type<K> keyType,
            Type<V> valueType) {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, Map<K, V>> modifyF(
                    Function<Tuple2<K, V>, App<F, Tuple2<K, V>>> f,
                    Map<K, V> source,
                    Applicative<F, ?> applicative) {
                App<F, LinkedHashMap<K, V>> acc = applicative.of(new LinkedHashMap<>());
                for (Map.Entry<K, V> entry : source.entrySet()) {
                    acc = applicative.map2(
                            acc,
                            f.apply(Tuple2.of(entry.getKey(), entry.getValue())),
                            (map, next) -> {
                                LinkedHashMap<K, V> copy = new LinkedHashMap<>(map);
                                copy.put(next.first(), next.second());
                                return copy;
                            });
                }
                return applicative.map(map -> map, acc);
            }

            @Override
            public Maybe<PointFreeOptic<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>>> typedOptic() {
                return Maybe.some(PointFreeOptic.mapEntries(keyType, valueType));
            }
        };
    }

    public static <A> Traversal<Set<A>, Set<A>, A, A> forSet() {
        return new GeneratedTraversal<>(GeneratedTraversal.SET, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }

    public static <A> Traversal<A[], A[], A, A> forArray(Class<A> componentType) {
        return new GeneratedTraversal<>(GeneratedTraversal.ARRAY, componentType) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }
}

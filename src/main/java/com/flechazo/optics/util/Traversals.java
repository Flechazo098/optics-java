package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.generated.GeneratedTraversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Traversals {
    private Traversals() {
    }

    public static <A> PTraversal<A, A, A, A> filtered(Predicate<? super A> predicate) {
        PTraversal<A, A, A, A> direct = new PTraversal<>() {
            @Override
            public <F extends K1>
            App<F, A> modifyF(
                    Function<A, App<F, A>> f,
                    A source,
                    Applicative<F, ?> applicative) {
                return predicate.test(source) ? f.apply(source) : applicative.of(source);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.opaque("filteredTraversal", null));
    }

    public static <A> PTraversal<List<A>, List<A>, A, A> forList() {
        PTraversal<List<A>, List<A>, A, A> direct = new GeneratedTraversal<>(GeneratedTraversal.LIST, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
        return OpticPrograms.traversal(
                direct, OpticPrograms.structured("listTraversal", null));
    }

    public static <A> PTraversal<List<A>, List<A>, A, A> forList(TypeToken<A> elementType) {
        return forList(Types.witness(elementType));
    }

    public static <A> PTraversal<List<A>, List<A>, A, A> forList(Type<A> elementType) {
        return OpticMetadata.optic(forList(), Maybe.some(PointFreeOptic.list(elementType)));
    }

    public static <A> PTraversal<Maybe<A>, Maybe<A>, A, A> forMaybe() {
        PTraversal<Maybe<A>, Maybe<A>, A, A> direct = new GeneratedTraversal<>(GeneratedTraversal.MAYBE, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
        return OpticPrograms.traversal(
                direct, OpticPrograms.structured("maybeTraversal", null));
    }

    public static <A> PTraversal<Maybe<A>, Maybe<A>, A, A> forMaybe(TypeToken<A> elementType) {
        return forMaybe(Types.witness(elementType));
    }

    public static <A> PTraversal<Maybe<A>, Maybe<A>, A, A> forMaybe(Type<A> elementType) {
        return OpticMetadata.optic(forMaybe(), Maybe.some(PointFreeOptic.maybe(elementType)));
    }

    public static <K, V> PTraversal<Map<K, V>, Map<K, V>, V, V> forMapValues() {
        PTraversal<Map<K, V>, Map<K, V>, V, V> direct =
                new GeneratedTraversal<>(GeneratedTraversal.MAP_VALUES, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
        return OpticPrograms.traversal(
                direct, OpticPrograms.structured("mapValuesTraversal", null));
    }

    public static <K, V> PTraversal<Map<K, V>, Map<K, V>, V, V> forMapValues(
            TypeToken<K> keyType,
            TypeToken<V> valueType) {
        return forMapValues(Types.witness(keyType), Types.witness(valueType));
    }

    public static <K, V> PTraversal<Map<K, V>, Map<K, V>, V, V> forMapValues(
            Type<K> keyType,
            Type<V> valueType) {
        return OpticMetadata.optic(
                forMapValues(), Maybe.some(PointFreeOptic.mapValues(keyType, valueType)));
    }

    public static <K, V> PTraversal<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> forMapEntries() {
        return forMapEntries(Types.variable("K"), Types.variable("V"));
    }

    public static <K, V> PTraversal<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> forMapEntries(
            TypeToken<K> keyType,
            TypeToken<V> valueType) {
        return forMapEntries(Types.witness(keyType), Types.witness(valueType));
    }

    public static <K, V> PTraversal<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> forMapEntries(
            Type<K> keyType,
            Type<V> valueType) {
        PTraversal<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> traversal = new PTraversal<>() {
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
        };
        PTraversal<Map<K, V>, Map<K, V>, Tuple2<K, V>, Tuple2<K, V>> typed = OpticMetadata.optic(
                traversal, Maybe.some(PointFreeOptic.mapEntries(keyType, valueType)));
        return OpticPrograms.traversal(
                typed, OpticPrograms.structured("mapEntriesTraversal", null));
    }

    public static <A> PTraversal<Set<A>, Set<A>, A, A> forSet() {
        PTraversal<Set<A>, Set<A>, A, A> direct = new GeneratedTraversal<>(GeneratedTraversal.SET, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
        return OpticPrograms.traversal(
                direct, OpticPrograms.structured("setTraversal", null));
    }

    public static <A> PTraversal<A[], A[], A, A> forArray(Class<A> componentType) {
        PTraversal<A[], A[], A, A> direct = new GeneratedTraversal<>(GeneratedTraversal.ARRAY, componentType) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
        return OpticPrograms.traversal(
                direct, OpticPrograms.structured("arrayTraversal", componentType));
    }
}

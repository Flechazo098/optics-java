package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.internal.AccumulationBuffer;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.generated.GeneratedTraversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Traversals {
    private Traversals() {
    }

    public static <A> Traversal<A, A> filtered(Predicate<? super A> predicate) {
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
        return Traversal.from(OpticPrograms.traversal(
                direct,
                OpticPrograms.opaque("filteredTraversal", null)));
    }

    public static <A> Traversal<List<A>, A> forList() {
        return Traversal.from(Traversals.pForList());
    }

    public static <A, B> PTraversal<List<A>, List<B>, A, B> pForList() {
        PTraversal<List<A>, List<B>, A, B> direct =
                new GeneratedTraversal<>(GeneratedTraversal.LIST, null) {
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
                direct,
                OpticPrograms.structured("listTraversal", null));
    }

    public static <A> Traversal<List<A>, A> forList(TypeToken<A> elementType) {
        return forList(Types.witness(elementType));
    }

    public static <A> Traversal<List<A>, A> forList(Type<A> elementType) {
        return Traversal.from(Traversals.pForList(elementType, elementType));
    }

    public static <A, B> PTraversal<List<A>, List<B>, A, B> pForList(
            TypeToken<A> elementType,
            TypeToken<B> targetElementType) {
        return pForList(Types.witness(elementType), Types.witness(targetElementType));
    }

    public static <A, B> PTraversal<List<A>, List<B>, A, B> pForList(
            Type<A> elementType,
            Type<B> targetElementType) {
        return OpticMetadata.optic(
                Traversals.pForList(),
                Maybe.some(PointFreeOptic.list(elementType, targetElementType)));
    }

    public static <A> Traversal<Maybe<A>, A> forMaybe() {
        return Traversal.from(Traversals.pForMaybe());
    }

    public static <A, B> PTraversal<Maybe<A>, Maybe<B>, A, B> pForMaybe() {
        PTraversal<Maybe<A>, Maybe<B>, A, B> direct =
                new GeneratedTraversal<>(GeneratedTraversal.MAYBE, null) {
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
                direct,
                OpticPrograms.structured("maybeTraversal", null));
    }

    public static <A> Traversal<Maybe<A>, A> forMaybe(TypeToken<A> elementType) {
        return forMaybe(Types.witness(elementType));
    }

    public static <A> Traversal<Maybe<A>, A> forMaybe(Type<A> elementType) {
        return Traversal.from(Traversals.pForMaybe(elementType, elementType));
    }

    public static <A, B> PTraversal<Maybe<A>, Maybe<B>, A, B> pForMaybe(
            TypeToken<A> elementType,
            TypeToken<B> targetElementType) {
        return pForMaybe(Types.witness(elementType), Types.witness(targetElementType));
    }

    public static <A, B> PTraversal<Maybe<A>, Maybe<B>, A, B> pForMaybe(
            Type<A> elementType,
            Type<B> targetElementType) {
        return OpticMetadata.optic(
                Traversals.pForMaybe(),
                Maybe.some(PointFreeOptic.maybe(elementType, targetElementType)));
    }

    public static <K, V> Traversal<Map<K, V>, V> forMapValues() {
        return Traversal.from(Traversals.pForMapValues());
    }

    public static <K, A, B> PTraversal<Map<K, A>, Map<K, B>, A, B> pForMapValues() {
        PTraversal<Map<K, A>, Map<K, B>, A, B> direct =
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
                direct,
                OpticPrograms.structured("mapValuesTraversal", null));
    }

    public static <K, V> Traversal<Map<K, V>, V> forMapValues(
            TypeToken<K> keyType,
            TypeToken<V> valueType) {
        return forMapValues(Types.witness(keyType), Types.witness(valueType));
    }

    public static <K, V> Traversal<Map<K, V>, V> forMapValues(
            Type<K> keyType,
            Type<V> valueType) {
        return Traversal.from(Traversals.pForMapValues(keyType, valueType, valueType));
    }

    public static <K, A, B> PTraversal<Map<K, A>, Map<K, B>, A, B> pForMapValues(
            TypeToken<K> keyType,
            TypeToken<A> valueType,
            TypeToken<B> targetValueType) {
        return pForMapValues(
                Types.witness(keyType), Types.witness(valueType), Types.witness(targetValueType));
    }

    public static <K, A, B> PTraversal<Map<K, A>, Map<K, B>, A, B> pForMapValues(
            Type<K> keyType,
            Type<A> valueType,
            Type<B> targetValueType) {
        return OpticMetadata.optic(
                Traversals.pForMapValues(),
                Maybe.some(PointFreeOptic.mapValues(keyType, valueType, targetValueType)));
    }

    public static <K, V> Traversal<Map<K, V>, Tuple2<K, V>> forMapEntries() {
        return Traversal.from(Traversals.pForMapEntries());
    }

    public static <K, V> Traversal<Map<K, V>, Tuple2<K, V>> forMapEntries(
            TypeToken<K> keyType,
            TypeToken<V> valueType) {
        return forMapEntries(Types.witness(keyType), Types.witness(valueType));
    }

    public static <K, V> Traversal<Map<K, V>, Tuple2<K, V>> forMapEntries(
            Type<K> keyType,
            Type<V> valueType) {
        return Traversal.from(Traversals.pForMapEntries(
                keyType, valueType, keyType, valueType));
    }

    public static <K, V, K2, V2>
    PTraversal<Map<K, V>, Map<K2, V2>, Tuple2<K, V>, Tuple2<K2, V2>> pForMapEntries() {
        return pForMapEntries(
                Types.variable("K"),
                Types.variable("V"),
                Types.variable("K2"),
                Types.variable("V2"));
    }

    public static <K, V, K2, V2>
    PTraversal<Map<K, V>, Map<K2, V2>, Tuple2<K, V>, Tuple2<K2, V2>> pForMapEntries(
            TypeToken<K> keyType,
            TypeToken<V> valueType,
            TypeToken<K2> targetKeyType,
            TypeToken<V2> targetValueType) {
        return pForMapEntries(
                Types.witness(keyType),
                Types.witness(valueType),
                Types.witness(targetKeyType),
                Types.witness(targetValueType));
    }

    public static <K, V, K2, V2>
    PTraversal<Map<K, V>, Map<K2, V2>, Tuple2<K, V>, Tuple2<K2, V2>> pForMapEntries(
            Type<K> keyType,
            Type<V> valueType,
            Type<K2> targetKeyType,
            Type<V2> targetValueType) {
        PTraversal<Map<K, V>, Map<K2, V2>, Tuple2<K, V>, Tuple2<K2, V2>> traversal =
                new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Map<K2, V2>> modifyF(
                    Function<Tuple2<K, V>, App<F, Tuple2<K2, V2>>> f,
                    Map<K, V> source,
                    Applicative<F, ?> applicative) {
                App<F, AccumulationBuffer<Tuple2<K2, V2>>> acc =
                        applicative.of(AccumulationBuffer.empty());
                for (Map.Entry<K, V> entry : source.entrySet()) {
                    acc = applicative.map2(
                            acc,
                            f.apply(Tuple2.of(entry.getKey(), entry.getValue())),
                            AccumulationBuffer::prepend);
                }
                return applicative.map(values -> {
                    LinkedHashMap<K2, V2> result = new LinkedHashMap<>(source.size());
                    for (Tuple2<K2, V2> entry : values.toList()) {
                        result.put(entry.first(), entry.second());
                    }
                    return Collections.unmodifiableMap(result);
                }, acc);
            }
        };
        PTraversal<Map<K, V>, Map<K2, V2>, Tuple2<K, V>, Tuple2<K2, V2>> typed =
                OpticMetadata.optic(
                        traversal,
                        Maybe.some(PointFreeOptic.mapEntries(
                                keyType, valueType, targetKeyType, targetValueType)));
        return OpticPrograms.traversal(
                typed,
                OpticPrograms.structured("mapEntriesTraversal", null));
    }

    public static <A> Traversal<Set<A>, A> forSet() {
        return Traversal.from(Traversals.<A, A>pForSet());
    }

    public static <A, B> PTraversal<Set<A>, Set<B>, A, B> pForSet() {
        PTraversal<Set<A>, Set<B>, A, B> direct =
                new GeneratedTraversal<>(GeneratedTraversal.SET, null) {
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
                direct,
                OpticPrograms.structured("setTraversal", null));
    }

    public static <A> Traversal<A[], A> forArray(Class<A> componentType) {
        return Traversal.from(Traversals.pForArray(componentType, componentType));
    }

    public static <A, B> PTraversal<A[], B[], A, B> pForArray(
            Class<A> sourceComponentType,
            Class<B> targetComponentType) {
        PTraversal<A[], B[], A, B> direct =
                new GeneratedTraversal<>(GeneratedTraversal.ARRAY, targetComponentType) {
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
                direct,
                OpticPrograms.structured(
                        "arrayTraversal", List.of(sourceComponentType, targetComponentType)));
    }
}

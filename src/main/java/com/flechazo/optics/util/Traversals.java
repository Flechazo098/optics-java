package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.business.data.Chain;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.generated.GeneratedTraversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides traversals for standard value and container types.
 */
public final class Traversals {
    private Traversals() {
    }

    /**
     * Creates a traversal that focuses a value only when it satisfies a predicate.
     *
     * @param <A> the value type
     * @param predicate the condition determining whether the value is focused
     * @return a traversal with at most one focus
     */
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

    /**
     * Creates a traversal over the elements of a list in encounter order.
     *
     * @param <A> the element type
     * @return a traversal over every list element
     */
    public static <A> Traversal<List<A>, A> forList() {
        return Traversal.from(Traversals.pForList());
    }

    /**
     * Creates a polymorphic traversal over the elements of a list in encounter order.
     *
     * @param <A> the source element type
     * @param <B> the replacement element type
     * @return a traversal that rebuilds a list of replacement elements
     */
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

    /**
     * Creates a typed traversal over the elements of a list.
     *
     * @param <A> the element type
     * @param elementType the runtime description of the element type
     * @return a list traversal with the supplied type metadata
     */
    public static <A> Traversal<List<A>, A> forList(TypeToken<A> elementType) {
        return forList(Types.witness(elementType));
    }

    /**
     * Creates a typed traversal over the elements of a list.
     *
     * @param <A> the element type
     * @param elementType the runtime witness for the element type
     * @return a list traversal with the supplied type metadata
     */
    public static <A> Traversal<List<A>, A> forList(Type<A> elementType) {
        return Traversal.from(Traversals.pForList(elementType, elementType));
    }

    /**
     * Creates a typed polymorphic traversal over the elements of a list.
     *
     * @param <A> the source element type
     * @param <B> the replacement element type
     * @param elementType the runtime description of the source element type
     * @param targetElementType the runtime description of the replacement element type
     * @return a list traversal with the supplied type metadata
     */
    public static <A, B> PTraversal<List<A>, List<B>, A, B> pForList(
            TypeToken<A> elementType,
            TypeToken<B> targetElementType) {
        return pForList(Types.witness(elementType), Types.witness(targetElementType));
    }

    /**
     * Creates a typed polymorphic traversal over the elements of a list.
     *
     * @param <A> the source element type
     * @param <B> the replacement element type
     * @param elementType the runtime witness for the source element type
     * @param targetElementType the runtime witness for the replacement element type
     * @return a list traversal with the supplied type metadata
     */
    public static <A, B> PTraversal<List<A>, List<B>, A, B> pForList(
            Type<A> elementType,
            Type<B> targetElementType) {
        return OpticMetadata.optic(
                Traversals.pForList(),
                Maybe.some(PointFreeOptic.list(elementType, targetElementType)));
    }

    /**
     * Creates a traversal over the value of a defined {@link Maybe}.
     *
     * @param <A> the value type
     * @return a traversal with no focus for an empty value
     */
    public static <A> Traversal<Maybe<A>, A> forMaybe() {
        return Traversal.from(Traversals.pForMaybe());
    }

    /**
     * Creates a polymorphic traversal over the value of a defined {@link Maybe}.
     *
     * @param <A> the source value type
     * @param <B> the replacement value type
     * @return a traversal that preserves absence
     */
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

    /**
     * Creates a typed traversal over the value of a defined {@link Maybe}.
     *
     * @param <A> the value type
     * @param elementType the runtime description of the value type
     * @return a maybe traversal with the supplied type metadata
     */
    public static <A> Traversal<Maybe<A>, A> forMaybe(TypeToken<A> elementType) {
        return forMaybe(Types.witness(elementType));
    }

    /**
     * Creates a typed traversal over the value of a defined {@link Maybe}.
     *
     * @param <A> the value type
     * @param elementType the runtime witness for the value type
     * @return a maybe traversal with the supplied type metadata
     */
    public static <A> Traversal<Maybe<A>, A> forMaybe(Type<A> elementType) {
        return Traversal.from(Traversals.pForMaybe(elementType, elementType));
    }

    /**
     * Creates a typed polymorphic traversal over the value of a defined {@link Maybe}.
     *
     * @param <A> the source value type
     * @param <B> the replacement value type
     * @param elementType the runtime description of the source value type
     * @param targetElementType the runtime description of the replacement value type
     * @return a maybe traversal with the supplied type metadata
     */
    public static <A, B> PTraversal<Maybe<A>, Maybe<B>, A, B> pForMaybe(
            TypeToken<A> elementType,
            TypeToken<B> targetElementType) {
        return pForMaybe(Types.witness(elementType), Types.witness(targetElementType));
    }

    /**
     * Creates a typed polymorphic traversal over the value of a defined {@link Maybe}.
     *
     * @param <A> the source value type
     * @param <B> the replacement value type
     * @param elementType the runtime witness for the source value type
     * @param targetElementType the runtime witness for the replacement value type
     * @return a maybe traversal with the supplied type metadata
     */
    public static <A, B> PTraversal<Maybe<A>, Maybe<B>, A, B> pForMaybe(
            Type<A> elementType,
            Type<B> targetElementType) {
        return OpticMetadata.optic(
                Traversals.pForMaybe(),
                Maybe.some(PointFreeOptic.maybe(elementType, targetElementType)));
    }

    /**
     * Creates a traversal over map values in entry encounter order.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a traversal that preserves keys
     */
    public static <K, V> Traversal<Map<K, V>, V> forMapValues() {
        return Traversal.from(Traversals.pForMapValues());
    }

    /**
     * Creates a polymorphic traversal over map values in entry encounter order.
     *
     * @param <K> the unchanged key type
     * @param <A> the source value type
     * @param <B> the replacement value type
     * @return a traversal that preserves keys and rebuilds the values
     */
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

    /**
     * Creates a typed traversal over map values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyType the runtime description of the key type
     * @param valueType the runtime description of the value type
     * @return a map-value traversal with the supplied type metadata
     */
    public static <K, V> Traversal<Map<K, V>, V> forMapValues(
            TypeToken<K> keyType,
            TypeToken<V> valueType) {
        return forMapValues(Types.witness(keyType), Types.witness(valueType));
    }

    /**
     * Creates a typed traversal over map values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyType the runtime witness for the key type
     * @param valueType the runtime witness for the value type
     * @return a map-value traversal with the supplied type metadata
     */
    public static <K, V> Traversal<Map<K, V>, V> forMapValues(
            Type<K> keyType,
            Type<V> valueType) {
        return Traversal.from(Traversals.pForMapValues(keyType, valueType, valueType));
    }

    /**
     * Creates a typed polymorphic traversal over map values.
     *
     * @param <K> the unchanged key type
     * @param <A> the source value type
     * @param <B> the replacement value type
     * @param keyType the runtime description of the key type
     * @param valueType the runtime description of the source value type
     * @param targetValueType the runtime description of the replacement value type
     * @return a map-value traversal with the supplied type metadata
     */
    public static <K, A, B> PTraversal<Map<K, A>, Map<K, B>, A, B> pForMapValues(
            TypeToken<K> keyType,
            TypeToken<A> valueType,
            TypeToken<B> targetValueType) {
        return pForMapValues(
                Types.witness(keyType), Types.witness(valueType), Types.witness(targetValueType));
    }

    /**
     * Creates a typed polymorphic traversal over map values.
     *
     * @param <K> the unchanged key type
     * @param <A> the source value type
     * @param <B> the replacement value type
     * @param keyType the runtime witness for the key type
     * @param valueType the runtime witness for the source value type
     * @param targetValueType the runtime witness for the replacement value type
     * @return a map-value traversal with the supplied type metadata
     */
    public static <K, A, B> PTraversal<Map<K, A>, Map<K, B>, A, B> pForMapValues(
            Type<K> keyType,
            Type<A> valueType,
            Type<B> targetValueType) {
        return OpticMetadata.optic(
                Traversals.pForMapValues(),
                Maybe.some(PointFreeOptic.mapValues(keyType, valueType, targetValueType)));
    }

    /**
     * Creates a traversal over map entries represented as key-value tuples.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a traversal over every map entry
     */
    public static <K, V> Traversal<Map<K, V>, Tuple2<K, V>> forMapEntries() {
        return Traversal.from(Traversals.pForMapEntries());
    }

    /**
     * Creates a typed traversal over map entries represented as key-value tuples.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyType the runtime description of the key type
     * @param valueType the runtime description of the value type
     * @return a map-entry traversal with the supplied type metadata
     */
    public static <K, V> Traversal<Map<K, V>, Tuple2<K, V>> forMapEntries(
            TypeToken<K> keyType,
            TypeToken<V> valueType) {
        return forMapEntries(Types.witness(keyType), Types.witness(valueType));
    }

    /**
     * Creates a typed traversal over map entries represented as key-value tuples.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyType the runtime witness for the key type
     * @param valueType the runtime witness for the value type
     * @return a map-entry traversal with the supplied type metadata
     */
    public static <K, V> Traversal<Map<K, V>, Tuple2<K, V>> forMapEntries(
            Type<K> keyType,
            Type<V> valueType) {
        return Traversal.from(Traversals.pForMapEntries(
                keyType, valueType, keyType, valueType));
    }

    /**
     * Creates a polymorphic traversal over map entries represented as key-value tuples.
     *
     * @param <K> the source key type
     * @param <V> the source value type
     * @param <K2> the replacement key type
     * @param <V2> the replacement value type
     * @return a traversal that rebuilds a map from the replacement entries
     */
    public static <K, V, K2, V2>
    PTraversal<Map<K, V>, Map<K2, V2>, Tuple2<K, V>, Tuple2<K2, V2>> pForMapEntries() {
        return pForMapEntries(
                Types.variable("K"),
                Types.variable("V"),
                Types.variable("K2"),
                Types.variable("V2"));
    }

    /**
     * Creates a typed polymorphic traversal over map entries.
     *
     * @param <K> the source key type
     * @param <V> the source value type
     * @param <K2> the replacement key type
     * @param <V2> the replacement value type
     * @param keyType the runtime description of the source key type
     * @param valueType the runtime description of the source value type
     * @param targetKeyType the runtime description of the replacement key type
     * @param targetValueType the runtime description of the replacement value type
     * @return a map-entry traversal with the supplied type metadata
     */
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

    /**
     * Creates a typed polymorphic traversal over map entries.
     *
     * <p>If multiple replacement entries contain equal keys, the last encountered value is retained.
     *
     * @param <K> the source key type
     * @param <V> the source value type
     * @param <K2> the replacement key type
     * @param <V2> the replacement value type
     * @param keyType the runtime witness for the source key type
     * @param valueType the runtime witness for the source value type
     * @param targetKeyType the runtime witness for the replacement key type
     * @param targetValueType the runtime witness for the replacement value type
     * @return a map-entry traversal with the supplied type metadata
     */
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
                        App<F, Chain<Tuple2<K2, V2>>> acc =
                                applicative.of(Chain.empty());
                        for (Map.Entry<K, V> entry : source.entrySet()) {
                            acc = applicative.map2(
                                    acc,
                                    f.apply(Tuple2.of(entry.getKey(), entry.getValue())),
                                    Chain::append);
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

    /**
     * Creates a traversal over set elements in encounter order.
     *
     * @param <A> the element type
     * @return a traversal over every distinct set element
     */
    public static <A> Traversal<Set<A>, A> forSet() {
        return Traversal.from(Traversals.pForSet());
    }

    /**
     * Creates a polymorphic traversal over set elements in encounter order.
     *
     * <p>Equal replacement elements are represented once in the rebuilt set.
     *
     * @param <A> the source element type
     * @param <B> the replacement element type
     * @return a traversal that rebuilds a set of replacement elements
     */
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

    /**
     * Creates a traversal over every element of an array in index order.
     *
     * @param <A> the component type
     * @param componentType the runtime array component type
     * @return a traversal that rebuilds an array with the same component type
     */
    public static <A> Traversal<A[], A> forArray(Class<A> componentType) {
        return Traversal.from(Traversals.pForArray(componentType, componentType));
    }

    /**
     * Creates a polymorphic traversal over every element of an array in index order.
     *
     * @param <A> the source component type
     * @param <B> the replacement component type
     * @param sourceComponentType the runtime source array component type
     * @param targetComponentType the runtime replacement array component type
     * @return a traversal that rebuilds an array of {@code targetComponentType}
     */
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

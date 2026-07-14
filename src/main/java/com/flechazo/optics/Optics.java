package com.flechazo.optics;

import com.flechazo.optics.generated.ClassFileOptics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides generated optic families for records and sealed hierarchies.
 */
public final class Optics {
    private Optics() {
    }

    /**
     * Returns lenses for every component of a record.
     *
     * @param <S> the record type
     * @param recordType the record class
     * @return an unmodifiable component-name map in declaration order
     * @throws IllegalArgumentException if {@code recordType} is not a supported record
     */
    public static <S> Map<String, Lens<S, ?>> lenses(Class<S> recordType) {
        LinkedHashMap<String, Lens<S, ?>> result = new LinkedHashMap<>();
        ClassFileOptics.lenses(recordType).forEach((name, lens) -> result.put(name, lens(lens)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns getters for every component of a record.
     *
     * @param <S> the record type
     * @param recordType the record class
     * @return a component-name map in declaration order
     * @throws IllegalArgumentException if {@code recordType} is not a supported record
     */
    public static <S> Map<String, Getter<S, ?>> getters(Class<S> recordType) {
        return ClassFileOptics.getters(recordType);
    }

    /**
     * Returns setters for every component of a record.
     *
     * @param <S> the record type
     * @param recordType the record class
     * @return an unmodifiable component-name map in declaration order
     * @throws IllegalArgumentException if {@code recordType} is not a supported record
     */
    public static <S> Map<String, Setter<S, ?>> setters(Class<S> recordType) {
        LinkedHashMap<String, Setter<S, ?>> result = new LinkedHashMap<>();
        ClassFileOptics.setters(recordType).forEach((name, setter) -> result.put(name, setter(setter)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns folds for every traversable component of a record.
     *
     * @param <S> the record type
     * @param recordType the record class
     * @return a component-name map in declaration order
     * @throws IllegalArgumentException if {@code recordType} is not a supported record
     */
    public static <S> Map<String, Fold<S, ?>> folds(Class<S> recordType) {
        return ClassFileOptics.folds(recordType);
    }

    /**
     * Returns traversals for every supported traversable component of a record.
     *
     * @param <S> the record type
     * @param recordType the record class
     * @return an unmodifiable component-name map in declaration order
     * @throws IllegalArgumentException if {@code recordType} is not a supported record
     */
    public static <S> Map<String, Traversal<S, ?>> traversals(Class<S> recordType) {
        LinkedHashMap<String, Traversal<S, ?>> result = new LinkedHashMap<>();
        ClassFileOptics.traversals(recordType).forEach((name, traversal) -> result.put(name, traversal(traversal)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns subtype prisms for the permitted subclasses of a sealed type.
     *
     * @param <S> the sealed base type
     * @param sealedType the sealed base class
     * @return an unmodifiable map from permitted classes to subtype prisms
     * @throws IllegalArgumentException if {@code sealedType} is not supported
     */
    public static <S> Map<Class<? extends S>, Prism<S, ? extends S>> subtypes(
            Class<S> sealedType) {
        LinkedHashMap<Class<? extends S>, Prism<S, ? extends S>> result = new LinkedHashMap<>();
        ClassFileOptics.prisms(sealedType).forEach((type, prism) -> result.put(type, prism(prism)));
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S> Lens<S, ?> lens(PLens<S, S, ?, ?> lens) {
        return Lens.from((PLens) lens);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S> Setter<S, ?> setter(PSetter<S, S, ?, ?> setter) {
        return Setter.from((PSetter) setter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S> Traversal<S, ?> traversal(PTraversal<S, S, ?, ?> traversal) {
        return Traversal.from((PTraversal) traversal);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S> Prism<S, ? extends S> prism(PPrism<S, S, ? extends S, ? extends S> prism) {
        return Prism.from((PPrism) prism);
    }
}

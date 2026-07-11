package com.flechazo.optics.generated;

import com.flechazo.optics.*;
import com.flechazo.optics.focus.FocusPath;
import com.google.common.collect.ImmutableMap;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassFileOptics {
    private static final ConcurrentHashMap<Class<?>, Map<String, Getter<?, ?>>> GENERATED_GETTERS =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Map<String, PSetter<?, ?, ?, ?>>> GENERATED_SETTERS =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Map<String, Fold<?, ?>>> GENERATED_FOLDS =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Map<String, FocusPath<?, ?>>> GENERATED_FOCUS =
            new ConcurrentHashMap<>();

    private ClassFileOptics() {
    }

    public static byte[] generatedHostBytes(Class<?> sourceType) {
        return RecordOptics.generateLensHostBytes(sourceType);
    }

    public static <S> Map<String, PLens<S, S, ?, ?>> lenses(Class<S> recordType) {
        return RecordOptics.recordLenses(recordType);
    }

    public static <S> Map<String, PLens<S, S, ?, ?>> lenses(Class<S> recordType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return RecordOptics.recordLenses(recordType, lookup);
    }

    public static <S, A> PLens<S, S, A, A> lens(Class<S> recordType, LensGetter<S, A> getter) {
        return RecordOptics.recordLens(recordType, getter);
    }

    public static <S, A> PLens<S, S, A, A> lens(
            Class<S> recordType, LensGetter<S, A> getter, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return RecordOptics.recordLens(recordType, getter, lookup);
    }

    public static <S> Map<String, Getter<S, ?>> getters(Class<S> recordType) {
        return getters(recordType, MethodHandles.lookup());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, Getter<S, ?>> getters(Class<S> recordType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return (Map) GENERATED_GETTERS.computeIfAbsent(recordType, ignored -> createGetters(recordType, lookup));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Getter<?, ?>> createGetters(Class<?> recordType, MethodHandles.Lookup lookup) {
        Map<String, PLens> lenses = (Map) lenses((Class) recordType, lookup);
        LinkedHashMap<String, Getter<?, ?>> result = new LinkedHashMap<>();
        lenses.forEach((name, lens) -> result.put(name, lens.asGetter()));
        return ImmutableMap.copyOf(result);
    }

    public static <S> Map<String, PSetter<S, S, ?, ?>> setters(Class<S> recordType) {
        return setters(recordType, MethodHandles.lookup());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, PSetter<S, S, ?, ?>> setters(Class<S> recordType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return (Map) GENERATED_SETTERS.computeIfAbsent(recordType, ignored -> createSetters(recordType, lookup));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, PSetter<?, ?, ?, ?>> createSetters(Class<?> recordType, MethodHandles.Lookup lookup) {
        Map<String, PLens> lenses = (Map) lenses((Class) recordType, lookup);
        LinkedHashMap<String, PSetter<?, ?, ?, ?>> result = new LinkedHashMap<>();
        lenses.forEach((name, lens) -> result.put(name, lens.asSetter()));
        return ImmutableMap.copyOf(result);
    }

    public static <S> Map<String, Fold<S, ?>> folds(Class<S> recordType) {
        return folds(recordType, MethodHandles.lookup());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, Fold<S, ?>> folds(Class<S> recordType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return (Map) GENERATED_FOLDS.computeIfAbsent(recordType, ignored -> createFolds(recordType, lookup));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Fold<?, ?>> createFolds(Class<?> recordType, MethodHandles.Lookup lookup) {
        Map<String, PLens> lenses = (Map) lenses((Class) recordType, lookup);
        LinkedHashMap<String, Fold<?, ?>> result = new LinkedHashMap<>();
        lenses.forEach((name, lens) -> result.put(name, lens.asFold()));
        return ImmutableMap.copyOf(result);
    }

    public static <S> Map<String, FocusPath<S, ?>> focus(Class<S> recordType) {
        return focus(recordType, MethodHandles.lookup());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, FocusPath<S, ?>> focus(Class<S> recordType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return (Map) GENERATED_FOCUS.computeIfAbsent(recordType, ignored -> createFocus(recordType, lookup));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, FocusPath<?, ?>> createFocus(Class<?> recordType, MethodHandles.Lookup lookup) {
        Map<String, PLens> lenses = (Map) lenses((Class) recordType, lookup);
        LinkedHashMap<String, FocusPath<?, ?>> result = new LinkedHashMap<>();
        lenses.forEach((name, lens) -> result.put(name, FocusPath.of(lens)));
        return ImmutableMap.copyOf(result);
    }

    public static <S> Map<Class<? extends S>, PPrism<S, S, ? extends S, ? extends S>> prisms(Class<S> sealedType) {
        return RecordOptics.sealedSubtypePrisms(sealedType);
    }

    public static <S> Map<Class<? extends S>, PPrism<S, S, ? extends S, ? extends S>> prisms(
            Class<S> sealedType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return RecordOptics.sealedSubtypePrisms(sealedType, lookup);
    }

    public static <S> Map<String, PTraversal<S, S, ?, ?>> traversals(Class<S> recordType) {
        return RecordOptics.recordTraversals(recordType);
    }

    public static <S> Map<String, PTraversal<S, S, ?, ?>> traversals(Class<S> recordType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return RecordOptics.recordTraversals(recordType, lookup);
    }
}

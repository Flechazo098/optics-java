package com.flechazo.optics.generated;

import com.flechazo.optics.*;
import com.flechazo.optics.focus.FocusPath;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassFileOptics {
    private static final ConcurrentHashMap<Class<?>, Map<String, Getter<?, ?>>> GENERATED_GETTERS =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Map<String, Setter<?, ?>>> GENERATED_SETTERS =
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

    public static <S> Map<String, Lens<S, ?>> lenses(Class<S> recordType) {
        return RecordOptics.recordLenses(recordType);
    }

    public static <S, A> Lens<S, A> lens(Class<S> recordType, LensGetter<S, A> getter) {
        return RecordOptics.recordLens(recordType, getter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, Getter<S, ?>> getters(Class<S> recordType) {
        return (Map) GENERATED_GETTERS.computeIfAbsent(recordType, ClassFileOptics::createGetters);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Getter<?, ?>> createGetters(Class<?> recordType) {
        Map<String, Lens> lenses = (Map) lenses((Class) recordType);
        LinkedHashMap<String, Getter<?, ?>> result = new LinkedHashMap<>();
        lenses.forEach((name, lens) -> result.put(name, lens.asGetter()));
        return Map.copyOf(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, Setter<S, ?>> setters(Class<S> recordType) {
        return (Map) GENERATED_SETTERS.computeIfAbsent(recordType, ClassFileOptics::createSetters);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Setter<?, ?>> createSetters(Class<?> recordType) {
        Map<String, Lens> lenses = (Map) lenses((Class) recordType);
        LinkedHashMap<String, Setter<?, ?>> result = new LinkedHashMap<>();
        lenses.forEach((name, lens) -> result.put(name, lens.asSetter()));
        return Map.copyOf(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, Fold<S, ?>> folds(Class<S> recordType) {
        return (Map) GENERATED_FOLDS.computeIfAbsent(recordType, ClassFileOptics::createFolds);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Fold<?, ?>> createFolds(Class<?> recordType) {
        Map<String, Lens> lenses = (Map) lenses((Class) recordType);
        LinkedHashMap<String, Fold<?, ?>> result = new LinkedHashMap<>();
        lenses.forEach((name, lens) -> result.put(name, lens.asFold()));
        return Map.copyOf(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, FocusPath<S, ?>> focus(Class<S> recordType) {
        return (Map) GENERATED_FOCUS.computeIfAbsent(recordType, ClassFileOptics::createFocus);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, FocusPath<?, ?>> createFocus(Class<?> recordType) {
        Map<String, Lens> lenses = (Map) lenses((Class) recordType);
        LinkedHashMap<String, FocusPath<?, ?>> result = new LinkedHashMap<>();
        lenses.forEach((name, lens) -> result.put(name, FocusPath.of(lens)));
        return Map.copyOf(result);
    }

    public static <S> Map<Class<? extends S>, Prism<S, ? extends S>> prisms(Class<S> sealedType) {
        return RecordOptics.sealedSubtypePrisms(sealedType);
    }

    public static <S> Map<String, Traversal<S, ?>> traversals(Class<S> recordType) {
        return RecordOptics.recordTraversals(recordType);
    }
}

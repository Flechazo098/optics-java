package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.optics.*;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticMetadata;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class OpticLowering {
    private OpticLowering() {
    }

    public static <S, A> PointFreeOptic<S, S, A, A> lens(Object key, PLens<S, S, A, A> lens) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(lens, "lens");
        return PointFreeOptic.lens(LensPath.of(key, lens));
    }

    public static <S, A> PointFreeOptic<S, S, A, A> lens(
            Object key, PLens<S, S, A, A> lens, TypeToken<S> sourceType, TypeToken<A> focusType) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(lens, "lens");
        return PointFreeOptic.lens(LensPath.of(key, lens), sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> affine(Object key, PAffine<S, S, A, A> affine) {
        return PointFreeOptic.affine(key, affine);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> affine(
            Object key, PAffine<S, S, A, A> affine, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return PointFreeOptic.affine(key, affine, sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> prism(Object key, PPrism<S, S, A, A> prism) {
        return PointFreeOptic.prism(key, prism);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> prism(
            Object key, PPrism<S, S, A, A> prism, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return PointFreeOptic.prism(key, prism, sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> traversal(Object key, PTraversal<S, S, A, A> traversal) {
        return PointFreeOptic.traversal(key, traversal);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> traversal(
            Object key, PTraversal<S, S, A, A> traversal, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return PointFreeOptic.traversal(key, traversal, sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> fold(Object key, Fold<S, A> fold) {
        return PointFreeOptic.fold(key, fold);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> fold(
            Object key, Fold<S, A> fold, TypeToken<S> sourceType, TypeToken<A> focusType) {
        return PointFreeOptic.fold(key, fold, sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> recordLens(Class<S> recordType, String componentName) {
        TypeToken<S> sourceType = TypeToken.of(recordType);
        TypeToken<A> focusType = componentType(recordType, componentName);
        return lens(componentName, RecordOptics.recordLens(recordType, componentName), sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> recordTraversal(Class<S> recordType, String componentName) {
        TypeToken<S> sourceType = TypeToken.of(recordType);
        TypeToken<A> focusType = traversalFocusType(recordType, componentName);
        return traversal(componentName, RecordOptics.recordTraversal(recordType, componentName), sourceType, focusType);
    }

    public static <S, A extends S> PointFreeOptic<S, S, A, A> subtype(Class<S> baseType, Class<A> subtype) {
        return PointFreeOptic.subtype(baseType, subtype);
    }

    public static <S, T, A, B> Maybe<PointFreeOptic<S, T, A, B>> lower(
            com.flechazo.optics.Optic<S, T, A, B> optic) {
        Objects.requireNonNull(optic, "optic");
        return OpticMetadata.optic(optic);
    }

    public static <S, T, A, B> PointFreeOptic<S, T, A, B> lowerOrThrow(
            com.flechazo.optics.Optic<S, T, A, B> optic) {
        return lower(optic).orElseGet(() -> {
            throw new IllegalArgumentException("Optic does not carry typed optimizer metadata");
        });
    }

    public static <S, T, A, B> PointFree<Function<S, T>> modify(
            PointFreeOptic<S, T, A, B> optic,
            String name,
            Function<? super A, ? extends B> function) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
        Function<A, B> typed = function::apply;
        PointFreeOpticTypes<S, T, A, B> types = optic.types();
        PointFree<Function<A, B>> plan = PointFree.fn(name, typed, types.focus(), types.replacement());
        return PointFree.opticApp(optic, plan);
    }

    public static <S, T, A, B> PointFree<Function<S, T>> modify(
            com.flechazo.optics.Optic<S, T, A, B> optic,
            String name,
            Function<? super A, ? extends B> function) {
        return modify(lowerOrThrow(optic), name, function);
    }

    public static <S, T, A, B> PointFree<Function<S, T>> set(
            PointFreeOptic<S, T, A, B> optic,
            B value) {
        return modify(optic, "set", ignored -> value);
    }

    public static <S, T, A, B> PointFree<Function<S, T>> set(
            com.flechazo.optics.Optic<S, T, A, B> optic,
            B value) {
        return set(lowerOrThrow(optic), value);
    }

    public static <S, T, A, B> T applyModify(
            PointFreeOptic<S, T, A, B> optic,
            String name,
            Function<? super A, ? extends B> function,
            S source) {
        return PointFreeOptimizer.optimize(modify(optic, name, function)).eval().apply(source);
    }

    public static <S, T, A, B> T applyModify(
            com.flechazo.optics.Optic<S, T, A, B> optic,
            String name,
            Function<? super A, ? extends B> function,
            S source) {
        return applyModify(lowerOrThrow(optic), name, function, source);
    }

    public static <S, T, A, B> T applySet(PointFreeOptic<S, T, A, B> optic, B value, S source) {
        return PointFreeOptimizer.optimize(set(optic, value)).eval().apply(source);
    }

    public static <S, T, A, B> T applySet(com.flechazo.optics.Optic<S, T, A, B> optic, B value, S source) {
        return applySet(lowerOrThrow(optic), value, source);
    }

    public static <S, A> FoldQuery<S, A, Maybe<A>, Maybe<A>> preview(Fold<S, A> fold) {
        return FoldQuery.preview(fold);
    }

    public static <S, A> FoldQuery<S, A, Maybe<A>, Maybe<A>> first(Fold<S, A> fold) {
        return FoldQuery.first(fold);
    }

    public static <S, A> FoldQuery<S, A, Integer, Integer> count(Fold<S, A> fold) {
        return FoldQuery.count(fold);
    }

    public static <S, A> FoldQuery<S, A, Boolean, Boolean> any(
            Fold<S, A> fold,
            Predicate<? super A> predicate) {
        return FoldQuery.any(fold, predicate);
    }

    public static <S, A> FoldQuery<S, A, Boolean, Boolean> all(
            Fold<S, A> fold,
            Predicate<? super A> predicate) {
        return FoldQuery.all(fold, predicate);
    }

    public static <S, A, M> FoldQuery<S, A, M, M> foldMap(
            Fold<S, A> fold,
            Monoid<M> monoid,
            Function<? super A, ? extends M> mapper) {
        return FoldQuery.foldMap(fold, monoid, mapper);
    }

    public static <A, B> PointFree<Function<A, B>> terminal(
            String name,
            Function<? super A, ? extends B> function) {
        return PointFree.fn(name, function);
    }

    private static <A> TypeToken<A> componentType(Class<?> recordType, String componentName) {
        return castTypeToken(TypeToken.of(component(recordType, componentName).getGenericType()));
    }

    private static <A> TypeToken<A> traversalFocusType(Class<?> recordType, String componentName) {
        RecordComponent component = component(recordType, componentName);
        Class<?> raw = component.getType();
        Type generic = component.getGenericType();
        if (raw.isArray()) {
            return castTypeToken(TypeToken.of((Type) raw.getComponentType()));
        }
        if (generic instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if ((List.class.isAssignableFrom(raw)
                    || Set.class.isAssignableFrom(raw)
                    || Maybe.class.isAssignableFrom(raw)
                    || Optional.class.isAssignableFrom(raw))
                    && arguments.length == 1) {
                return castTypeToken(TypeToken.of(arguments[0]));
            }
            if (Map.class.isAssignableFrom(raw) && arguments.length == 2) {
                return castTypeToken(TypeToken.of(arguments[1]));
            }
        }
        return castTypeToken(TypeToken.of((Type) raw));
    }

    private static RecordComponent component(Class<?> recordType, String componentName) {
        for (RecordComponent component : recordType.getRecordComponents()) {
            if (component.getName().equals(componentName)) {
                return component;
            }
        }
        throw new IllegalArgumentException(
                "Record component '" + componentName + "' not found on " + recordType.getName());
    }

    @SuppressWarnings("unchecked")
    private static <A> TypeToken<A> castTypeToken(TypeToken<?> type) {
        return (TypeToken<A>) type;
    }
}

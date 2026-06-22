package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.type.TypeRef;
import com.flechazo.optics.Affine;
import com.flechazo.optics.Fold;
import com.flechazo.optics.Lens;
import com.flechazo.optics.Prism;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.generated.RecordOptics;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class OpticLowering {
    private OpticLowering() {
    }

    public static <S, A> PointFreeOptic<S, S, A, A> lens(Object key, Lens<S, A> lens) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(lens, "lens");
        return PointFreeOptic.lens(LensPath.of(key, lens));
    }

    public static <S, A> PointFreeOptic<S, S, A, A> lens(
            Object key, Lens<S, A> lens, TypeRef<S> sourceType, TypeRef<A> focusType) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(lens, "lens");
        return PointFreeOptic.lens(LensPath.of(key, lens), sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> affine(Object key, Affine<S, A> affine) {
        return PointFreeOptic.affine(key, affine);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> affine(
            Object key, Affine<S, A> affine, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return PointFreeOptic.affine(key, affine, sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> prism(Object key, Prism<S, A> prism) {
        return PointFreeOptic.prism(key, prism);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> prism(
            Object key, Prism<S, A> prism, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return PointFreeOptic.prism(key, prism, sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> traversal(Object key, Traversal<S, A> traversal) {
        return PointFreeOptic.traversal(key, traversal);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> traversal(
            Object key, Traversal<S, A> traversal, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return PointFreeOptic.traversal(key, traversal, sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> fold(Object key, Fold<S, A> fold) {
        return PointFreeOptic.fold(key, fold);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> fold(
            Object key, Fold<S, A> fold, TypeRef<S> sourceType, TypeRef<A> focusType) {
        return PointFreeOptic.fold(key, fold, sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> recordLens(Class<S> recordType, String componentName) {
        TypeRef<S> sourceType = TypeRef.of(recordType);
        TypeRef<A> focusType = componentType(recordType, componentName);
        return lens(componentName, RecordOptics.recordLens(recordType, componentName), sourceType, focusType);
    }

    public static <S, A> PointFreeOptic<S, S, A, A> recordTraversal(Class<S> recordType, String componentName) {
        TypeRef<S> sourceType = TypeRef.of(recordType);
        TypeRef<A> focusType = traversalFocusType(recordType, componentName);
        return traversal(componentName, RecordOptics.recordTraversal(recordType, componentName), sourceType, focusType);
    }

    public static <S, A extends S> PointFreeOptic<S, S, A, A> subtype(Class<S> baseType, Class<A> subtype) {
        return PointFreeOptic.subtype(baseType, subtype);
    }

    public static <S, T, A, B> PointFree<Function<S, T>> modify(
            PointFreeOptic<S, T, A, B> optic,
            String name,
            Function<? super A, ? extends B> function) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
        Function<A, B> typed = function::apply;
        PointFree<Function<A, B>> plan = PointFree.fn(name, typed);
        if (optic.types().isDefined()) {
            PointFreeOpticTypes types = optic.types().get();
            plan = PointFree.fn(name, typed, types.focus(), types.replacement());
        }
        return PointFree.opticApp(optic, plan);
    }

    public static <S, T, A, B> PointFree<Function<S, T>> set(
            PointFreeOptic<S, T, A, B> optic,
            B value) {
        return modify(optic, "set", ignored -> value);
    }

    public static <S, T, A, B> T applyModify(
            PointFreeOptic<S, T, A, B> optic,
            String name,
            Function<? super A, ? extends B> function,
            S source) {
        return PointFreeOptimizer.optimize(modify(optic, name, function)).eval().apply(source);
    }

    public static <S, T, A, B> T applySet(PointFreeOptic<S, T, A, B> optic, B value, S source) {
        return PointFreeOptimizer.optimize(set(optic, value)).eval().apply(source);
    }

    public static <S, A> PointFree<Function<S, Maybe<A>>> preview(Fold<S, A> fold) {
        return PointFree.fn("preview", fold::preview);
    }

    public static <S, A, M> FoldQuery<S, A, M, M> foldMap(
            Fold<S, A> fold,
            Monoid<M> monoid,
            Function<? super A, ? extends M> mapper) {
        return FoldQuery.foldMap(fold, monoid, mapper);
    }

    private static <A> TypeRef<A> componentType(Class<?> recordType, String componentName) {
        return TypeRef.of(component(recordType, componentName).getGenericType());
    }

    private static <A> TypeRef<A> traversalFocusType(Class<?> recordType, String componentName) {
        RecordComponent component = component(recordType, componentName);
        Class<?> raw = component.getType();
        Type generic = component.getGenericType();
        if (raw.isArray()) {
            return TypeRef.of((Type) raw.getComponentType());
        }
        if (generic instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if ((List.class.isAssignableFrom(raw)
                    || Set.class.isAssignableFrom(raw)
                    || Maybe.class.isAssignableFrom(raw)
                    || Optional.class.isAssignableFrom(raw))
                    && arguments.length == 1) {
                return TypeRef.of(arguments[0]);
            }
            if (Map.class.isAssignableFrom(raw) && arguments.length == 2) {
                return TypeRef.of(arguments[1]);
            }
        }
        return TypeRef.of((Type) raw);
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
}

package com.flechazo.optics.generated;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.GeneratedTraversalRuntime;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.PTraversal;
import com.flechazo.hkt.internal.AccumulationBuffer;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

public abstract class GeneratedTraversal<S, T, A, B> implements PTraversal<S, T, A, B> {
    public static final int LIST = GeneratedTraversalRuntime.LIST;
    public static final int SET = GeneratedTraversalRuntime.SET;
    public static final int MAP_VALUES = GeneratedTraversalRuntime.MAP_VALUES;
    public static final int MAYBE = GeneratedTraversalRuntime.MAYBE;
    public static final int ARRAY = GeneratedTraversalRuntime.ARRAY;

    private final int kind;
    private final @Nullable Class<?> arrayComponentType;

    protected GeneratedTraversal(int kind, @Nullable Class<?> arrayComponentType) {
        this.kind = kind;
        this.arrayComponentType = arrayComponentType;
    }

    protected abstract Object getContainer(Object source);

    protected abstract Object setContainer(Object container, Object source);

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
        if (applicative == Maybe.applicative()) {
            Maybe<Object> modified =
                    sequenceMaybeApplicative(kind, arrayComponentType, (Function) f, getContainer(source));
            return modified.isDefined()
                    ? (App<F, T>) Maybe.ofNullable((T) setContainer(modified.get(), source))
                    : (App<F, T>) Maybe.none();
        }
        if (applicative == IdF.applicative()) {
            Object modified = GeneratedTraversalRuntime.modifyContainer(
                    kind,
                    arrayComponentType,
                    value -> IdF.get((App<IdF.Mu, Object>) ((Function) f).apply(value)),
                    getContainer(source));
            return (App<F, T>) new IdF<>((T) setContainer(modified, source));
        }
        App<F, Object> modified = sequence(kind, arrayComponentType, (Function) f, getContainer(source), applicative);
        return applicative.map(container -> (T) setContainer(container, source), modified);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final T modify(Function<? super A, ? extends B> f, S source) {
        Object modified = GeneratedTraversalRuntime.modifyContainer(kind, arrayComponentType, (Function) f, getContainer(source));
        return (T) setContainer(modified, source);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final List<A> getAll(S source) {
        return (List<A>) GeneratedTraversalRuntime.values(kind, getContainer(source));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Maybe<A> preview(S source) {
        List<?> values = GeneratedTraversalRuntime.values(kind, getContainer(source));
        return values.isEmpty() ? Maybe.none() : Maybe.some((A) values.getFirst());
    }

    @Override
    public final int length(S source) {
        return GeneratedTraversalRuntime.length(kind, getContainer(source));
    }

    @SuppressWarnings("rawtypes")
    private static <F extends K1> App<F, Object> sequence(
            int kind, @Nullable Class<?> arrayComponentType, Function<Object, App<F, Object>> f, Object container,
            Applicative<F, ?> applicative) {
        return switch (kind) {
            case LIST -> (App) sequenceList(f, (List<?>) container, applicative);
            case SET -> (App) sequenceSet(f, (Set<?>) container, applicative);
            case MAP_VALUES -> (App) sequenceMapValues(f, (Map<?, ?>) container, applicative);
            case MAYBE -> (App) sequenceMaybe(f, (Maybe<?>) container, applicative);
            case ARRAY -> sequenceArray(f, container, requireArrayComponentType(arrayComponentType), applicative);
            default -> throw unsupported(kind);
        };
    }

    private static <F extends K1> App<F, List<Object>> sequenceList(
            Function<Object, App<F, Object>> f, List<?> source, Applicative<F, ?> applicative) {
        App<F, AccumulationBuffer<Object>> acc = applicative.of(AccumulationBuffer.empty());
        for (Object value : source) {
            acc = applicative.map2(acc, f.apply(value), AccumulationBuffer::prepend);
        }
        return applicative.map(AccumulationBuffer::toList, acc);
    }

    private static <F extends K1> App<F, Set<Object>> sequenceSet(
            Function<Object, App<F, Object>> f, Set<?> source, Applicative<F, ?> applicative) {
        App<F, AccumulationBuffer<Object>> acc = applicative.of(AccumulationBuffer.empty());
        for (Object value : source) {
            acc = applicative.map2(acc, f.apply(value), AccumulationBuffer::prepend);
        }
        return applicative.map(
                values -> Collections.unmodifiableSet(new LinkedHashSet<>(values.toList())),
                acc);
    }

    private static <F extends K1> App<F, Map<Object, Object>> sequenceMapValues(
            Function<Object, App<F, Object>> f, Map<?, ?> source, Applicative<F, ?> applicative) {
        App<F, AccumulationBuffer<Tuple2<Object, Object>>> acc = applicative.of(AccumulationBuffer.empty());
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object key = entry.getKey();
            acc = applicative.map2(
                    acc,
                    f.apply(entry.getValue()),
                    (values, next) -> values.prepend(Tuple2.of(key, next)));
        }
        return applicative.map(values -> {
            LinkedHashMap<Object, Object> result = new LinkedHashMap<>(source.size());
            for (Tuple2<Object, Object> entry : values.toList()) {
                result.put(entry.first(), entry.second());
            }
            return Collections.unmodifiableMap(result);
        }, acc);
    }

    private static <F extends K1> App<F, Maybe<Object>> sequenceMaybe(
            Function<Object, App<F, Object>> f, Maybe<?> source, Applicative<F, ?> applicative) {
        return source.isDefined()
                ? applicative.map(Maybe::some, f.apply(source.get()))
                : applicative.of(Maybe.none());
    }

    private static <F extends K1> App<F, Object> sequenceArray(
            Function<Object, App<F, Object>> f, Object source, Class<?> componentType, Applicative<F, ?> applicative) {
        int length = Array.getLength(source);
        App<F, AccumulationBuffer<Object>> acc = applicative.of(AccumulationBuffer.empty());
        for (int i = 0; i < length; i++) {
            Object value = Array.get(source, i);
            acc = applicative.map2(acc, f.apply(value), AccumulationBuffer::prepend);
        }
        return applicative.map(values -> toArray(values.toList(), componentType), acc);
    }

    private static Maybe<Object> sequenceMaybeApplicative(
            int kind,
            @Nullable Class<?> arrayComponentType,
            Function<Object, App<Maybe.Mu, Object>> f,
            Object container) {
        return switch (kind) {
            case LIST -> sequenceListMaybe(f, (List<?>) container);
            case SET -> sequenceSetMaybe(f, (Set<?>) container);
            case MAP_VALUES -> sequenceMapValuesMaybe(f, (Map<?, ?>) container);
            case MAYBE -> sequenceMaybeMaybe(f, (Maybe<?>) container);
            case ARRAY -> sequenceArrayMaybe(f, container, requireArrayComponentType(arrayComponentType));
            default -> throw unsupported(kind);
        };
    }

    private static Maybe<Object> sequenceListMaybe(Function<Object, App<Maybe.Mu, Object>> f, List<?> source) {
        ArrayList<Object> next = new ArrayList<>(source.size());
        for (Object value : source) {
            Maybe<Object> item = (Maybe) f.apply(value);
            if (item.isEmpty()) {
                return Maybe.none();
            }
            next.add(item.get());
        }
        return Maybe.some(Collections.unmodifiableList(next));
    }

    private static Maybe<Object> sequenceSetMaybe(Function<Object, App<Maybe.Mu, Object>> f, Set<?> source) {
        LinkedHashSet<Object> next = new LinkedHashSet<>();
        for (Object value : source) {
            Maybe<Object> item = (Maybe) f.apply(value);
            if (item.isEmpty()) {
                return Maybe.none();
            }
            next.add(item.get());
        }
        return Maybe.some(Collections.unmodifiableSet(next));
    }

    private static Maybe<Object> sequenceMapValuesMaybe(
            Function<Object, App<Maybe.Mu, Object>> f, Map<?, ?> source) {
        LinkedHashMap<Object, Object> next = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Maybe<Object> item = (Maybe) f.apply(entry.getValue());
            if (item.isEmpty()) {
                return Maybe.none();
            }
            next.put(entry.getKey(), item.get());
        }
        return Maybe.some(Collections.unmodifiableMap(next));
    }

    private static Maybe<Object> sequenceMaybeMaybe(Function<Object, App<Maybe.Mu, Object>> f, Maybe<?> source) {
        if (source.isEmpty()) {
            return Maybe.some(Maybe.none());
        }
        Maybe<Object> item = (Maybe) f.apply(source.get());
        return item.isDefined() ? Maybe.some(Maybe.some(item.get())) : Maybe.none();
    }

    private static Maybe<Object> sequenceArrayMaybe(
            Function<Object, App<Maybe.Mu, Object>> f, Object source, Class<?> componentType) {
        int length = Array.getLength(source);
        Object next = Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            Maybe<Object> item = (Maybe) f.apply(Array.get(source, i));
            if (item.isEmpty()) {
                return Maybe.none();
            }
            Array.set(next, i, item.get());
        }
        return Maybe.some(next);
    }

    private static Object toArray(List<Object> values, Class<?> componentType) {
        Object result = Array.newInstance(componentType, values.size());
        for (int i = 0; i < values.size(); i++) {
            Array.set(result, i, values.get(i));
        }
        return result;
    }

    private static IllegalArgumentException unsupported(int kind) {
        return new IllegalArgumentException("Unsupported generated traversal kind: " + kind);
    }

    private static Class<?> requireArrayComponentType(@Nullable Class<?> componentType) {
        if (componentType == null) {
            throw new IllegalStateException("Array traversal requires a component type");
        }
        return componentType;
    }
}

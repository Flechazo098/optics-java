package com.flechazo.optics.generated;

import com.flechazo.hkt.*;
import com.flechazo.optics.Traversal;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

public abstract class GeneratedTraversal<S, A> implements Traversal<S, S, A, A> {
    public static final int LIST = 1;
    public static final int SET = 2;
    public static final int MAP_VALUES = 3;
    public static final int MAYBE = 4;
    public static final int ARRAY = 5;

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
    public final <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
        if (applicative == Maybe.applicative()) {
            Maybe<Object> modified =
                    sequenceMaybeApplicative(kind, arrayComponentType, (Function) f, getContainer(source));
            return modified.isDefined()
                    ? (App<F, S>) Maybe.ofNullable((S) setContainer(modified.get(), source))
                    : (App<F, S>) Maybe.none();
        }
        if (applicative == IdF.applicative()) {
            Object modified = modifyContainer(
                    kind,
                    arrayComponentType,
                    value -> IdF.get((App<IdF.Mu, Object>) ((Function) f).apply(value)),
                    getContainer(source));
            return (App<F, S>) new IdF<>((S) setContainer(modified, source));
        }
        App<F, Object> modified = sequence(kind, arrayComponentType, (Function) f, getContainer(source), applicative);
        return applicative.map(container -> (S) setContainer(container, source), modified);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final S modify(Function<? super A, ? extends A> f, S source) {
        Object modified = modifyContainer(kind, arrayComponentType, (Function) f, getContainer(source));
        return (S) setContainer(modified, source);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final List<A> getAll(S source) {
        return (List<A>) values(kind, getContainer(source));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Maybe<A> preview(S source) {
        List<?> values = values(kind, getContainer(source));
        return values.isEmpty() ? Maybe.none() : Maybe.some((A) values.getFirst());
    }

    @Override
    public final int length(S source) {
        return switch (kind) {
            case LIST -> ((List<?>) getContainer(source)).size();
            case SET -> ((Set<?>) getContainer(source)).size();
            case MAP_VALUES -> ((Map<?, ?>) getContainer(source)).size();
            case MAYBE -> ((Maybe<?>) getContainer(source)).isDefined() ? 1 : 0;
            case ARRAY -> Array.getLength(getContainer(source));
            default -> throw unsupported(kind);
        };
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
        App<F, List<Object>> acc = applicative.of(List.of());
        for (Object value : source) {
            acc =
                    applicative.map2(
                            acc,
                            f.apply(value),
                            (list, next) -> {
                                ArrayList<Object> copy = new ArrayList<>(list);
                                copy.add(next);
                                return ImmutableList.copyOf(copy);
                            });
        }
        return acc;
    }

    private static <F extends K1> App<F, Set<Object>> sequenceSet(
            Function<Object, App<F, Object>> f, Set<?> source, Applicative<F, ?> applicative) {
        App<F, LinkedHashSet<Object>> acc = applicative.of(new LinkedHashSet<>());
        for (Object value : source) {
            acc =
                    applicative.map2(
                            acc,
                            f.apply(value),
                            (set, next) -> {
                                LinkedHashSet<Object> copy = new LinkedHashSet<>(set);
                                copy.add(next);
                                return copy;
                            });
        }
        return applicative.map(ImmutableSet::copyOf, acc);
    }

    private static <F extends K1> App<F, Map<Object, Object>> sequenceMapValues(
            Function<Object, App<F, Object>> f, Map<?, ?> source, Applicative<F, ?> applicative) {
        App<F, LinkedHashMap<Object, Object>> acc = applicative.of(new LinkedHashMap<>());
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object key = entry.getKey();
            acc =
                    applicative.map2(
                            acc,
                            f.apply(entry.getValue()),
                            (map, next) -> {
                                LinkedHashMap<Object, Object> copy = new LinkedHashMap<>(map);
                                copy.put(key, next);
                                return copy;
                            });
        }
        return applicative.map(ImmutableMap::copyOf, acc);
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
        App<F, List<Object>> acc = applicative.of(new ArrayList<>(length));
        for (int i = 0; i < length; i++) {
            Object value = Array.get(source, i);
            acc =
                    applicative.map2(
                            acc,
                            f.apply(value),
                            (list, next) -> {
                                ArrayList<Object> copy = new ArrayList<>(list);
                                copy.add(next);
                                return copy;
                            });
        }
        return applicative.map(values -> toArray(values, componentType), acc);
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
        return Maybe.some(ImmutableList.copyOf(next));
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
        return Maybe.some(ImmutableSet.copyOf(next));
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
        return Maybe.some(ImmutableMap.copyOf(next));
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

    private static Object modifyContainer(
            int kind, @Nullable Class<?> arrayComponentType, Function<Object, Object> f, Object container) {
        return switch (kind) {
            case LIST -> modifyList(f, (List<?>) container);
            case SET -> modifySet(f, (Set<?>) container);
            case MAP_VALUES -> modifyMapValues(f, (Map<?, ?>) container);
            case MAYBE -> modifyMaybe(f, (Maybe<?>) container);
            case ARRAY -> modifyArray(f, container, requireArrayComponentType(arrayComponentType));
            default -> throw unsupported(kind);
        };
    }

    private static List<Object> modifyList(Function<Object, Object> f, List<?> source) {
        ArrayList<Object> next = new ArrayList<>(source.size());
        for (Object value : source) {
            next.add(f.apply(value));
        }
        return ImmutableList.copyOf(next);
    }

    private static Set<Object> modifySet(Function<Object, Object> f, Set<?> source) {
        LinkedHashSet<Object> next = new LinkedHashSet<>();
        for (Object value : source) {
            next.add(f.apply(value));
        }
        return ImmutableSet.copyOf(next);
    }

    private static Map<Object, Object> modifyMapValues(Function<Object, Object> f, Map<?, ?> source) {
        LinkedHashMap<Object, Object> next = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            next.put(entry.getKey(), f.apply(entry.getValue()));
        }
        return ImmutableMap.copyOf(next);
    }

    private static Maybe<Object> modifyMaybe(Function<Object, Object> f, Maybe<?> source) {
        return source.isDefined() ? Maybe.ofNullable(f.apply(source.get())) : Maybe.none();
    }

    private static Object modifyArray(Function<Object, Object> f, Object source, Class<?> componentType) {
        int length = Array.getLength(source);
        Object next = Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            Array.set(next, i, f.apply(Array.get(source, i)));
        }
        return next;
    }

    private static List<Object> values(int kind, Object container) {
        return switch (kind) {
            case LIST -> nullableList((List<?>) container);
            case SET -> nullableList((Set<?>) container);
            case MAP_VALUES -> nullableList(((Map<?, ?>) container).values());
            case MAYBE -> {
                Maybe<?> maybe = (Maybe<?>) container;
                yield maybe.isDefined() ? nullableSingletonList(maybe.get()) : List.of();
            }
            case ARRAY -> {
                int length = Array.getLength(container);
                ArrayList<Object> values = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    values.add(Array.get(container, i));
                }
                yield Collections.unmodifiableList(values);
            }
            default -> throw unsupported(kind);
        };
    }

    private static List<Object> nullableList(Collection<?> values) {
        //noinspection Java9CollectionFactory
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<Object> nullableSingletonList(@Nullable Object value) {
        ArrayList<Object> values = new ArrayList<>(1);
        values.add(value);
        return Collections.unmodifiableList(values);
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

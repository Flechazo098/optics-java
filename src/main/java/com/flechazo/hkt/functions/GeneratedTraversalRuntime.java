package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class GeneratedTraversalRuntime {
    public static final int LIST = 1;
    public static final int SET = 2;
    public static final int MAP_VALUES = 3;
    public static final int MAYBE = 4;
    public static final int ARRAY = 5;

    private GeneratedTraversalRuntime() {
    }

    public static Object modifyContainer(
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

    public static List<Object> values(int kind, Object container) {
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

    public static int length(int kind, Object container) {
        return switch (kind) {
            case LIST -> ((List<?>) container).size();
            case SET -> ((Set<?>) container).size();
            case MAP_VALUES -> ((Map<?, ?>) container).size();
            case MAYBE -> ((Maybe<?>) container).isDefined() ? 1 : 0;
            case ARRAY -> Array.getLength(container);
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

    private static List<Object> nullableList(Collection<?> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<Object> nullableSingletonList(@Nullable Object value) {
        ArrayList<Object> values = new ArrayList<>(1);
        values.add(value);
        return Collections.unmodifiableList(values);
    }

    public static Class<?> requireArrayComponentType(@Nullable Class<?> componentType) {
        if (componentType == null) {
            throw new IllegalStateException("Array traversal requires a component type");
        }
        return componentType;
    }

    private static IllegalArgumentException unsupported(int kind) {
        return new IllegalArgumentException("Unsupported generated traversal kind: " + kind);
    }
}

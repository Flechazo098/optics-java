package com.flechazo.hkt.functions;

import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Traversing;
import com.flechazo.hkt.business.util.OptionalOps;
import com.flechazo.optics.PTraversal;
import com.google.common.reflect.TypeToken;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public record RecordTraversalOpticElement(
        RecordLensOpticElement component,
        int containerKind,
        @Nullable Class<?> arrayComponentType,
        PTraversal<Object, Object, Object, Object> traversal)
        implements PointFreeOpticElement {
    public RecordTraversalOpticElement {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(traversal, "traversal");
        if (containerKind < GeneratedTraversalRuntime.LIST || containerKind > GeneratedTraversalRuntime.ARRAY) {
            throw new IllegalArgumentException("Unsupported generated traversal kind: " + containerKind);
        }
        if (containerKind == GeneratedTraversalRuntime.ARRAY && arrayComponentType == null) {
            throw new IllegalArgumentException("Array traversal requires a component type");
        }
    }

    public static RecordTraversalOpticElement of(
            Class<?> recordType,
            RecordComponent[] components,
            int componentIndex,
            int containerKind,
            PTraversal<Object, Object, Object, Object> traversal) {
        RecordComponent component = components[componentIndex];
        Class<?> arrayComponentType =
                containerKind == GeneratedTraversalRuntime.ARRAY ? component.getType().getComponentType() : null;
        return new RecordTraversalOpticElement(
                RecordLensOpticElement.of(recordType, components, componentIndex),
                containerKind,
                arrayComponentType,
                traversal);
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.TRAVERSAL;
    }

    @Override
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Traversing.Mu.TYPE_TOKEN);
    }

    @Override
    public Object key() {
        return component.key();
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        Object container = containerForTraversal(component.readComponent(source));
        Object modified =
                GeneratedTraversalRuntime.modifyContainer(containerKind, arrayComponentType, function, container);
        return component.rebuild(componentFromTraversal(modified), source);
    }

    public boolean optionalContainer() {
        return Optional.class.isAssignableFrom(component.componentType());
    }

    public Object containerForTraversal(Object rawContainer) {
        return optionalContainer() ? OptionalOps.toMaybe((Optional<?>) rawContainer) : rawContainer;
    }

    public Object componentFromTraversal(Object traversalContainer) {
        return optionalContainer() ? OptionalOps.fromMaybe((Maybe<?>) traversalContainer) : traversalContainer;
    }

    @Override
    public boolean sameOptic(PointFreeOpticElement other) {
        return other.untyped() instanceof RecordTraversalOpticElement(
                RecordLensOpticElement component1, int kind, Class<?> componentType,
                PTraversal<Object, Object, Object, Object> traversal1
        )
                && component.sameOptic(component1)
                && containerKind == kind
                && Objects.equals(arrayComponentType, componentType)
                && traversal == traversal1;
    }

    @Override
    public String toString() {
        return "RecordTraversalOpticElement[component="
                + component.recordType().getName()
                + "."
                + component.componentName()
                + ", containerKind="
                + containerKind
                + ", componentTypes="
                + Arrays.toString(component.componentTypes())
                + "]";
    }
}

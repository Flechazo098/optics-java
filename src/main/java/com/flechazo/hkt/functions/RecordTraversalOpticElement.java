package com.flechazo.hkt.functions;

import com.flechazo.hkt.K1;
import com.flechazo.hkt.Traversing;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.util.Optionals;
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
        Traversal<Object, Object, Object, Object> traversal)
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
            Traversal<Object, Object, Object, Object> traversal) {
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
        return optionalContainer() ? Optionals.toMaybe((Optional<?>) rawContainer) : rawContainer;
    }

    public Object componentFromTraversal(Object traversalContainer) {
        return optionalContainer() ? Optionals.fromMaybe((com.flechazo.hkt.Maybe<?>) traversalContainer) : traversalContainer;
    }

    @Override
    public boolean sameOptic(PointFreeOpticElement other) {
        return other.untyped() instanceof RecordTraversalOpticElement that
                && component.sameOptic(that.component)
                && containerKind == that.containerKind
                && Objects.equals(arrayComponentType, that.arrayComponentType)
                && traversal == that.traversal;
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

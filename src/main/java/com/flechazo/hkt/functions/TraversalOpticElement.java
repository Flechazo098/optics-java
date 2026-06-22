package com.flechazo.hkt.functions;

import com.flechazo.hkt.ProfunctorBound;
import com.flechazo.optics.Traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record TraversalOpticElement(Object key, Traversal<Object, Object> traversal) implements PointFreeOpticElement {
    public TraversalOpticElement {
        Objects.requireNonNull(key, "key");
    }

    public static TraversalOpticElement list() {
        return new TraversalOpticElement("list", null);
    }

    public static TraversalOpticElement of(Object key, Traversal<Object, Object> traversal) {
        return new TraversalOpticElement(key, Objects.requireNonNull(traversal, "traversal"));
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.TRAVERSAL;
    }

    @Override
    public Set<ProfunctorBound> bounds() {
        return Set.of(ProfunctorBound.TRAVERSING);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        if (traversal != null) {
            return traversal.modify(function, source);
        }
        if (!Objects.equals(key, "list")) {
            throw new IllegalStateException("Unknown traversal optic: " + key);
        }
        List<?> values = (List<?>) source;
        ArrayList<Object> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(function.apply(value));
        }
        return List.copyOf(result);
    }
}

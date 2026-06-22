package com.flechazo.hkt.functions;

import com.flechazo.hkt.ProfunctorBound;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record TraversalOpticElement(Object key) implements PointFreeOpticElement {
    public TraversalOpticElement {
        Objects.requireNonNull(key, "key");
    }

    public static TraversalOpticElement list() {
        return new TraversalOpticElement("list");
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

package com.flechazo.hkt.functions;

import com.flechazo.hkt.ProfunctorBound;
import com.flechazo.optics.Prism;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record PrismOpticElement(Object key, Prism<Object, Object> prism) implements PointFreeOpticElement {
    public PrismOpticElement {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(prism, "prism");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.PRISM;
    }

    @Override
    public Set<ProfunctorBound> bounds() {
        return Set.of(ProfunctorBound.CHOICE);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return prism.modify(function, source);
    }
}

package com.flechazo.hkt.functions;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record LensOpticElement(LensPath.Element element) implements PointFreeOpticElement {
    public LensOpticElement {
        Objects.requireNonNull(element, "element");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.LENS;
    }

    @Override
    public Set<PointFreeOpticBound> bounds() {
        return Set.of(PointFreeOpticBound.CARTESIAN);
    }

    @Override
    public Object key() {
        return element.key();
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return element.set(function.apply(element.get(source)), source);
    }

    @Override
    public boolean sameOptic(PointFreeOpticElement other) {
        return other.untyped() instanceof LensOpticElement(LensPath.Element element1) && element.equals(element1);
    }
}

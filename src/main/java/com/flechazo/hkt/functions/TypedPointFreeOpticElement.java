package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record TypedPointFreeOpticElement(
        PointFreeOpticElement element,
        PointFreeOpticTypes opticTypes) implements PointFreeOpticElement {
    public TypedPointFreeOpticElement {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(opticTypes, "opticTypes");
        if (element instanceof TypedPointFreeOpticElement) {
            throw new IllegalArgumentException("typed optic element cannot wrap another typed element");
        }
    }

    @Override
    public PointFreeOpticKind kind() {
        return element.kind();
    }

    @Override
    public Set<PointFreeOpticBound> bounds() {
        return element.bounds();
    }

    @Override
    public Maybe<PointFreeOpticTypes> types() {
        return Maybe.some(opticTypes);
    }

    @Override
    public PointFreeOpticElement untyped() {
        return element;
    }

    @Override
    public Object key() {
        return element.key();
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return element.modify(function, source);
    }

    @Override
    public boolean sameOptic(PointFreeOpticElement other) {
        return element.sameOptic(other);
    }
}

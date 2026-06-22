package com.flechazo.hkt.functions;

import com.flechazo.hkt.ProfunctorBound;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record SubtypeOpticElement(Class<?> subtype) implements PointFreeOpticElement {
    public SubtypeOpticElement {
        Objects.requireNonNull(subtype, "subtype");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.SUBTYPE;
    }

    @Override
    public Set<ProfunctorBound> bounds() {
        return Set.of(ProfunctorBound.AFFINE);
    }

    @Override
    public Object key() {
        return subtype;
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return subtype.isInstance(source) ? function.apply(source) : source;
    }
}

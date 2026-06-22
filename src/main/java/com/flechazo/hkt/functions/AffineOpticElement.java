package com.flechazo.hkt.functions;

import com.flechazo.hkt.ProfunctorBound;
import com.flechazo.optics.Affine;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record AffineOpticElement(Object key, Affine<Object, Object> affine) implements PointFreeOpticElement {
    public AffineOpticElement {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(affine, "affine");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.AFFINE;
    }

    @Override
    public Set<ProfunctorBound> bounds() {
        return Set.of(ProfunctorBound.AFFINE);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return affine.modify(function, source);
    }
}

package com.flechazo.hkt.functions;

import com.flechazo.hkt.ProfunctorBound;

import java.util.Set;
import java.util.function.Function;

public record AdapterOpticElement(Object key) implements PointFreeOpticElement {
    public AdapterOpticElement() {
        this("adapter");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.ADAPTER;
    }

    @Override
    public Set<ProfunctorBound> bounds() {
        return Set.of(ProfunctorBound.PROFUNCTOR);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return function.apply(source);
    }
}

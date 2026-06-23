package com.flechazo.hkt.functions;

import com.flechazo.hkt.K1;
import com.flechazo.hkt.Profunctor;
import com.google.common.reflect.TypeToken;

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
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Profunctor.Mu.TYPE_TOKEN);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return function.apply(source);
    }
}

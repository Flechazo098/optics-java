package com.flechazo.hkt.functions;

import com.flechazo.hkt.AffineP;
import com.flechazo.hkt.K1;
import com.google.common.reflect.TypeToken;

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
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(AffineP.Mu.TYPE_TOKEN);
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

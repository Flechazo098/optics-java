package com.flechazo.hkt.functions;

import com.flechazo.hkt.AffineP;
import com.flechazo.hkt.K1;
import com.flechazo.optics.Affine;
import com.google.common.reflect.TypeToken;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record AffineOpticElement<S, T, A, B>(Object key, Affine<S, T, A, B> affine) implements PointFreeOpticElement {
    public AffineOpticElement {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(affine, "affine");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.AFFINE;
    }

    @Override
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(AffineP.Mu.TYPE_TOKEN);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return affine.modify(value -> (B) (function.apply(value)), (S) source);
    }
}

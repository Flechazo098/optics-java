package com.flechazo.hkt.functions;

import com.flechazo.hkt.K1;
import com.google.common.reflect.TypeToken;

import java.util.Set;
import java.util.function.Function;

public sealed interface PointFreeOpticElement permits
        AdapterOpticElement,
        AffineOpticElement,
        FoldOpticElement,
        LensOpticElement,
        MapOpticElement,
        PrismOpticElement,
        ProductOpticElement,
        SubtypeOpticElement,
        SumOpticElement,
        TraversalOpticElement,
        TaggedOpticElement {
    PointFreeOpticKind kind();

    Set<TypeToken<? extends K1>> bounds();

    default PointFreeOpticElement untyped() {
        return this;
    }

    Object key();

    Object modify(Function<Object, Object> function, Object source);

    default boolean sameOptic(PointFreeOpticElement other) {
        PointFreeOpticElement left = untyped();
        PointFreeOpticElement right = other.untyped();
        return left.getClass() == right.getClass() && left.key().equals(right.key());
    }
}

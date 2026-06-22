package com.flechazo.hkt.functions;

import com.flechazo.hkt.Pair;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record ProductOpticElement(ProductSide side) implements PointFreeOpticElement {
    public ProductOpticElement {
        Objects.requireNonNull(side, "side");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.PRODUCT;
    }

    @Override
    public Set<PointFreeOpticBound> bounds() {
        return Set.of(PointFreeOpticBound.CARTESIAN);
    }

    @Override
    public Object key() {
        return side;
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        Pair<?, ?> pair = (Pair<?, ?>) source;
        return switch (side) {
            case FIRST -> Pair.of(function.apply(pair.first()), pair.second());
            case SECOND -> Pair.of(pair.first(), function.apply(pair.second()));
        };
    }
}

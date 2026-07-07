package com.flechazo.hkt.functions;

import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Tuple2;
import com.google.common.reflect.TypeToken;

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
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Cartesian.Mu.TYPE_TOKEN);
    }

    @Override
    public Object key() {
        return side;
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        Tuple2<?, ?> tuple = (Tuple2<?, ?>) source;
        return switch (side) {
            case FIRST -> Tuple2.of(function.apply(tuple.first()), tuple.second());
            case SECOND -> Tuple2.of(tuple.first(), function.apply(tuple.second()));
        };
    }
}

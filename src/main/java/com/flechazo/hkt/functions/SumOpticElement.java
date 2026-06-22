package com.flechazo.hkt.functions;

import com.flechazo.hkt.Either;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record SumOpticElement(SumSide side) implements PointFreeOpticElement {
    public SumOpticElement {
        Objects.requireNonNull(side, "side");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.SUM;
    }

    @Override
    public Set<PointFreeOpticBound> bounds() {
        return Set.of(PointFreeOpticBound.COCARTESIAN);
    }

    @Override
    public Object key() {
        return side;
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        Either<?, ?> either = (Either<?, ?>) source;
        return switch (side) {
            case LEFT -> either.isLeft() ? Either.left(function.apply(either.left())) : either;
            case RIGHT -> either.isRight() ? Either.right(function.apply(either.right())) : either;
        };
    }
}

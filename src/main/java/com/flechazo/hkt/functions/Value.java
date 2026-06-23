package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record Value<A>(A value, Type<A> type) implements PointFree<A> {
    public Value {
        Objects.requireNonNull(type, "type");
    }

    @Override
    public A eval() {
        return value;
    }

    @Override
    @NonNull
    public String toString() {
        return "value(" + value + ")";
    }
}

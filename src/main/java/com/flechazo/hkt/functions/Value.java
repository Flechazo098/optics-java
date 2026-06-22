package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

public record Value<A>(A value) implements PointFree<A> {
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

package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record CompositePointFreeFold<S, A>(TypedFold<S, A> typed) implements PointFreeFold<S, A> {
    public CompositePointFreeFold {
        Objects.requireNonNull(typed, "typed");
    }

    @Override
    @NonNull
    public String toString() {
        return typed.node().toString();
    }
}

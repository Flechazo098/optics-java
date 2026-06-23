package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record CompositePointFreeOptic<S, T, A, B>(
        TypedOptic<S, T, A, B> typed) implements PointFreeOptic<S, T, A, B> {
    public CompositePointFreeOptic {
        Objects.requireNonNull(typed, "typed");
    }

    @Override
    @NonNull
    public String toString() {
        return String.join(".", elements().stream().map(element -> String.valueOf(element.key())).toList());
    }
}

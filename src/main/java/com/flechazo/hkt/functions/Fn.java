package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record Fn<A, B>(String name, Function<? super A, ? extends B> function)
        implements PointFree<Function<A, B>> {
    public Fn {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
    }

    @Override
    public Function<A, B> eval() {
        return function::apply;
    }

    @Override
    @NonNull
    public String toString() {
        return name;
    }
}

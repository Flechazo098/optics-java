package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record OpticApp<S, T, A, B>(
        PointFreeOptic<S, T, A, B> optic,
        PointFree<? extends Function<?, ?>> function)
        implements PointFree<Function<S, T>> {
    public OpticApp {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(function, "function");
    }

    @Override
    public Function<S, T> eval() {
        Function<A, B> modifier = cast(function.eval());
        return source -> optic.modify(modifier, source);
    }

    @Override
    @NonNull
    public String toString() {
        return "(ap " + optic + " " + function + ")";
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }
}

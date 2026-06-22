package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record OpticApp<S, A>(PointFreeOptic<S> optic, PointFree<? extends Function<?, ?>> function)
        implements PointFree<Function<S, S>> {
    public OpticApp {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(function, "function");
    }

    @Override
    public Function<S, S> eval() {
        Function<Object, Object> modifier = cast(function.eval());
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

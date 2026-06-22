package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record AppExpr<A, B>(PointFree<Function<A, B>> function, PointFree<A> argument)
        implements PointFree<B> {
    public AppExpr {
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(argument, "argument");
    }

    @Override
    public B eval() {
        return function.eval().apply(argument.eval());
    }

    @Override
    @NonNull
    public String toString() {
        return "(ap " + function + " " + argument + ")";
    }
}

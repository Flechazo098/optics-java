package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record AppExpr<A, B>(
        PointFree<Function<A, B>> function,
        PointFree<A> argument,
        Type<B> type)
        implements PointFree<B> {
    public AppExpr(PointFree<Function<A, B>> function, PointFree<A> argument) {
        this(function, argument, PointFreeTypes.applicationType(function, argument));
    }

    public AppExpr {
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(argument, "argument");
        Objects.requireNonNull(type, "type");
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

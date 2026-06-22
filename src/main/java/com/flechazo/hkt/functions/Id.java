package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.function.Function;

public record Id<A>() implements PointFree<Function<A, A>> {
    @Override
    public Function<A, A> eval() {
        return Function.identity();
    }

    @Override
    @NonNull
    public String toString() {
        return "id";
    }
}

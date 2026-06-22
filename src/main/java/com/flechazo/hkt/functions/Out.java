package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record Out<A>(RecursiveFamily family, int index) implements PointFree<Function<A, A>> {
    public Out {
        Objects.requireNonNull(family, "family");
        family.checkIndex(index);
    }

    @Override
    public Function<A, A> eval() {
        return Function.identity();
    }

    @Override
    @NonNull
    public String toString() {
        return "Out[" + family.name() + "#" + index + "]";
    }
}

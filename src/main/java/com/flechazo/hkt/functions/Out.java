package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record Out<A>(RecursiveFamily family, int index, Type<A> recursiveType) implements PointFree<Function<A, A>> {
    public Out {
        Objects.requireNonNull(family, "family");
        family.checkIndex(index);
        Objects.requireNonNull(recursiveType, "recursiveType");
    }

    @Override
    public Function<A, A> eval() {
        return Function.identity();
    }

    @Override
    public Type<Function<A, A>> type() {
        return Types.function(recursiveType, recursiveType);
    }

    @Override
    @NonNull
    public String toString() {
        return "Out[" + recursiveType + "]";
    }
}

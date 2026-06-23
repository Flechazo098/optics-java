package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record In<A>(RecursiveFamily family, int index, Type<A> recursiveType) implements PointFree<Function<A, A>> {
    public In {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(recursiveType, "recursiveType");
        family.checkIndex(index);
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
        return "In[" + recursiveType + "]";
    }
}

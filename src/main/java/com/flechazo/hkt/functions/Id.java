package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record Id<A>(Type<A> valueType) implements PointFree<Function<A, A>> {
    public Id {
        Objects.requireNonNull(valueType, "valueType");
    }

    @Override
    public Function<A, A> eval() {
        return Function.identity();
    }

    @Override
    public Type<Function<A, A>> type() {
        return Types.function(valueType, valueType);
    }

    @Override
    @NonNull
    public String toString() {
        return "id";
    }
}

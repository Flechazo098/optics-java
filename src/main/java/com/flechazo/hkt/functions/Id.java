package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record Id<A>(
        Type<A> valueType,
        Type<Function<A, A>> type)
        implements PointFree<Function<A, A>> {
    public Id(Type<A> valueType) {
        this(valueType, Types.function(valueType, valueType));
    }

    public Id {
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(type, "type");
    }

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

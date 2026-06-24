package com.flechazo.hkt.functions;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record Bang<A>(
        Type<A> sourceType,
        Type<Function<A, Unit>> type)
        implements PointFree<Function<A, Unit>> {
    public Bang(Type<A> sourceType) {
        this(sourceType, Types.function(sourceType, Types.UNIT));
    }

    public Bang {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(type, "type");
    }

    @Override
    public Function<A, Unit> eval() {
        return ignored -> Unit.INSTANCE;
    }

    @Override
    @NonNull
    public String toString() {
        return "!";
    }
}

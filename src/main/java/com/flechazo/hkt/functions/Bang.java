package com.flechazo.hkt.functions;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record Bang<A>(Type<A> sourceType) implements PointFree<Function<A, Unit>> {
    public Bang {
        Objects.requireNonNull(sourceType, "sourceType");
    }

    @Override
    public Function<A, Unit> eval() {
        return ignored -> Unit.INSTANCE;
    }

    @Override
    public Type<Function<A, Unit>> type() {
        return Types.function(sourceType, Types.UNIT);
    }

    @Override
    @NonNull
    public String toString() {
        return "!";
    }
}

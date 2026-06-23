package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record OpticTransformer<S, T, A, B>(PointFreeOptic<S, T, A, B> optic)
        implements PointFree<Function<Function<A, B>, Function<S, T>>> {
    public OpticTransformer {
        Objects.requireNonNull(optic, "optic");
    }

    @Override
    public Function<Function<A, B>, Function<S, T>> eval() {
        return modifier -> source -> optic.modify(modifier, source);
    }

    @Override
    public Type<Function<Function<A, B>, Function<S, T>>> type() {
        return Types.function(
                Types.function(optic.focusType(), optic.replacementType()),
                Types.function(optic.sourceType(), optic.targetType()));
    }

    @Override
    @NonNull
    public String toString() {
        return "Optic[" + optic + "]";
    }

}

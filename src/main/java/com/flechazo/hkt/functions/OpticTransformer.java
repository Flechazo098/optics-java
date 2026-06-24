package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record OpticTransformer<S, T, A, B>(
        PointFreeOptic<S, T, A, B> optic,
        Type<Function<Function<A, B>, Function<S, T>>> type)
        implements PointFree<Function<Function<A, B>, Function<S, T>>> {
    public OpticTransformer(PointFreeOptic<S, T, A, B> optic) {
        this(optic, opticTransformerType(optic));
    }

    public OpticTransformer {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(type, "type");
    }

    @Override
    public Function<Function<A, B>, Function<S, T>> eval() {
        return modifier -> source -> optic.modify(modifier, source);
    }

    @Override
    @NonNull
    public String toString() {
        return "Optic[" + optic + "]";
    }

    private static <S, T, A, B> Type<Function<Function<A, B>, Function<S, T>>> opticTransformerType(
            PointFreeOptic<S, T, A, B> optic) {
        return Types.function(
                Types.function(optic.focusType(), optic.replacementType()),
                Types.function(optic.sourceType(), optic.targetType()));
    }
}

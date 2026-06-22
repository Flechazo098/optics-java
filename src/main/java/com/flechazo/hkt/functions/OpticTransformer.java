package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.TypeExpr;
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
    public Maybe<TypeExpr> type() {
        return optic.types().map(types -> TypeExpr.function(
                TypeExpr.function(types.focus(), types.replacement()),
                TypeExpr.function(types.source(), types.target())));
    }

    public <S2, T2> OpticTransformer<S2, T2, A, B> castOuter(
            TypeExpr sourceType,
            TypeExpr targetType) {
        Maybe<PointFreeOpticTypes> types = optic.types();
        if (types.isEmpty()) {
            throw new IllegalStateException("Cannot cast untyped optic transformer outer type");
        }
        return new OpticTransformer<>(
                new CompositePointFreeOptic<>(optic.elements(), Maybe.some(types.get().castOuter(sourceType, targetType))));
    }

    @Override
    @NonNull
    public String toString() {
        return "Optic[" + optic + "]";
    }

}

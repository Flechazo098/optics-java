package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record TypedPointFree<A>(PointFree<A> expression, Type<A> expressionType) implements PointFree<A> {
    public TypedPointFree {
        Objects.requireNonNull(expression, "expression");
        expressionType = PointFreeTypes.validateExplicit(expression, expressionType);
    }

    @Override
    public A eval() {
        return expression.eval();
    }

    @Override
    public Type<A> type() {
        return expressionType;
    }

    @Override
    @NonNull
    public String toString() {
        return expression + " : " + expressionType;
    }
}

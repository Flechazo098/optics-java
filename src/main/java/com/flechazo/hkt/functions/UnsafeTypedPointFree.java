package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record UnsafeTypedPointFree<A>(PointFree<A> expression, Type<A> expressionType) implements PointFree<A> {
    public UnsafeTypedPointFree {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(expressionType, "expressionType");
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
        return expression + " :unsafe " + expressionType;
    }
}

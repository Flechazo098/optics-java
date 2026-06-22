package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.TypeExpr;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record TypedPointFree<A>(PointFree<A> expression, TypeExpr expressionType) implements PointFree<A> {
    public TypedPointFree {
        Objects.requireNonNull(expression, "expression");
        expressionType = PointFreeTypes.validateExplicit(expression, expressionType);
        if (expression instanceof TypedPointFree<?> typed) {
            expression = cast(typed.expression());
        }
        if (expression instanceof UnsafeTypedPointFree<?> typed) {
            expression = cast(typed.expression());
        }
    }

    @Override
    public A eval() {
        return expression.eval();
    }

    @Override
    public Maybe<TypeExpr> type() {
        return Maybe.some(expressionType);
    }

    @Override
    @NonNull
    public String toString() {
        return expression + " : " + expressionType;
    }

    @SuppressWarnings("unchecked")
    private static <A> PointFree<A> cast(PointFree<?> expression) {
        return (PointFree<A>) expression;
    }
}

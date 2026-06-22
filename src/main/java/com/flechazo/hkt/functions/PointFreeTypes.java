package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;
import com.flechazo.hkt.type.TypeUnifier;

import java.util.Objects;
import java.util.function.Function;

final class PointFreeTypes {
    private static final TypeExpr UNIT = TypeRef.of(Unit.class).expr();

    private PointFreeTypes() {
    }

    static Maybe<TypeExpr.FunctionType> functionType(PointFree<?> expression) {
        Objects.requireNonNull(expression, "expression");
        return expression.type().flatMap(PointFreeTypes::asFunction);
    }

    static Maybe<TypeExpr.FunctionType> asFunction(TypeExpr type) {
        return type instanceof TypeExpr.FunctionType functionType
                ? Maybe.some(functionType)
                : Maybe.none();
    }

    static boolean compatible(TypeExpr left, TypeExpr right) {
        return TypeUnifier.unify(left, right).isDefined();
    }

    static Maybe<TypeExpr> applicationType(PointFree<?> function, PointFree<?> argument) {
        return functionType(function).flatMap(functionType ->
                argument.type()
                        .filter(argumentType -> compatible(functionType.argument(), argumentType))
                        .map(ignored -> functionType.result()));
    }

    static Maybe<TypeExpr> opticAppType(
            PointFreeOptic<?, ?, ?, ?> optic,
            PointFree<?> modifier) {
        return optic.types().flatMap(opticTypes ->
                functionType(modifier)
                        .filter(functionType -> compatible(opticTypes.focus(), functionType.argument()))
                        .filter(functionType -> compatible(opticTypes.replacement(), functionType.result()))
                        .map(ignored -> TypeExpr.function(opticTypes.source(), opticTypes.target())));
    }

    static Maybe<TypeExpr> compositionType(PointFree<?> outer, PointFree<?> inner) {
        return functionType(outer).flatMap(outerType ->
                functionType(inner).flatMap(outerType::compose).map(type -> type));
    }

    static TypeExpr validateExplicit(PointFree<?> expression, TypeExpr type) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(type, "type");
        Maybe<TypeExpr> existing = expression.type();
        if (existing.isDefined() && !compatible(existing.get(), type)) {
            throw new IllegalArgumentException("point-free type conflict: existing "
                    + existing.get() + ", requested " + type);
        }

        if (expression instanceof Id<?> || expression instanceof In<?> || expression instanceof Out<?>
                || expression instanceof CataPlan<?>) {
            TypeExpr.FunctionType function = requireFunction(type, expression);
            if (!compatible(function.argument(), function.result())) {
                throw new IllegalArgumentException(expression + " requires an endomorphic function type, got " + type);
            }
        } else if (expression instanceof Bang<?>) {
            TypeExpr.FunctionType function = requireFunction(type, expression);
            if (!compatible(function.result(), UNIT)) {
                throw new IllegalArgumentException("bang requires a Unit result type, got " + type);
            }
        } else if (expression instanceof Fn<?, ?> || expression instanceof Comp<?, ?>
                || expression instanceof OpticApp<?, ?, ?, ?> || expression instanceof OpticTransformer<?, ?, ?, ?>
                || expression instanceof FoldQuery<?, ?, ?, ?>) {
            requireFunction(type, expression);
        }
        return type;
    }

    static <A> PointFree<A> retypeLike(PointFree<?> source, PointFree<A> replacement) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(replacement, "replacement");
        return retypeWithSourceType(source.type(), replacement);
    }

    static <A> PointFree<A> retypeAs(Maybe<TypeExpr> sourceType, PointFree<A> replacement) {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(replacement, "replacement");
        return retypeWithSourceType(sourceType, replacement);
    }

    private static <A> PointFree<A> retypeWithSourceType(Maybe<TypeExpr> sourceType, PointFree<A> replacement) {
        if (sourceType.isEmpty()) {
            return replacement;
        }
        Maybe<TypeExpr> replacementType = replacement.type();
        if (replacementType.isDefined()) {
            if (!compatible(sourceType.get(), replacementType.get())) {
                throw new IllegalStateException("rewrite type conflict: source "
                        + sourceType.get() + ", replacement " + replacementType.get());
            }
            return replacement;
        }
        return replacement.withType(sourceType.get());
    }

    static Maybe<TypeExpr> pairCompositionType(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        return compositionType(outer, inner);
    }

    private static TypeExpr.FunctionType requireFunction(TypeExpr type, PointFree<?> expression) {
        Maybe<TypeExpr.FunctionType> function = asFunction(type);
        if (function.isEmpty()) {
            throw new IllegalArgumentException(expression + " requires a function type, got " + type);
        }
        return function.get();
    }
}

package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.Func;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.TypeUnifier;
import com.flechazo.hkt.type.Types;

import java.util.Objects;
import java.util.function.Function;

final class PointFreeTypes {
    private PointFreeTypes() {
    }

    static Func<?, ?> functionType(PointFree<?> expression) {
        Objects.requireNonNull(expression, "expression");
        return requireFunction(expression.type(), expression);
    }

    static Maybe<Func<?, ?>> asFunction(Type<?> type) {
        return type instanceof Func<?, ?> functionType
                ? Maybe.some(functionType)
                : Maybe.none();
    }

    static boolean compatible(Type<?> left, Type<?> right) {
        if (left == right || left.equals(right)) {
            return true;
        }
        return TypeUnifier.unify(left, right).isDefined();
    }

    static <B> Type<B> applicationType(PointFree<?> function, PointFree<?> argument) {
        Func<?, ?> functionType = functionType(function);
        if (!compatible(functionType.input(), argument.type())) {
            throw new IllegalArgumentException("application type mismatch: function input "
                    + functionType.input() + ", argument " + argument.type());
        }
        return cast(functionType.output());
    }

    static <S, T, A, B> Type<Function<S, T>> opticAppType(
            PointFreeOptic<S, T, A, B> optic,
            PointFree<?> modifier) {
        PointFreeOpticTypes<S, T, A, B> opticTypes = optic.types();
        Func<?, ?> functionType = functionType(modifier);
        if (!compatible(opticTypes.focus(), functionType.input())
                || !compatible(opticTypes.replacement(), functionType.output())) {
            throw new IllegalArgumentException("optic modifier type mismatch: optic "
                    + opticTypes.focus() + " -> " + opticTypes.replacement()
                    + ", modifier " + functionType);
        }
        return Types.function(opticTypes.source(), opticTypes.target());
    }

    static <A, C> Type<Function<A, C>> compositionType(PointFree<?> outer, PointFree<?> inner) {
        Func<?, ?> outerType = functionType(outer);
        Func<?, ?> innerType = functionType(inner);
        if (!compatible(innerType.output(), outerType.input())) {
            throw new IllegalArgumentException("composition type mismatch: inner output "
                    + innerType.output() + ", outer input " + outerType.input());
        }
        return Types.function(cast(innerType.input()), cast(outerType.output()));
    }

    static <A, C> Type<Function<A, C>> compositionType(
            RewriteContext context,
            PointFree<?> outer,
            PointFree<?> inner) {
        Func<?, ?> outerType = context.functionType(outer);
        Func<?, ?> innerType = context.functionType(inner);
        if (!context.compatible(innerType.output(), outerType.input())) {
            throw new IllegalArgumentException("composition type mismatch: inner output "
                    + innerType.output() + ", outer input " + outerType.input());
        }
        return Types.function(cast(innerType.input()), cast(outerType.output()));
    }

    static <A> Type<A> validateExplicit(PointFree<A> expression, Type<A> type) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(type, "type");
        if (!compatible(expression.type(), type)) {
            throw new IllegalArgumentException("point-free type conflict: existing "
                    + expression.type() + ", requested " + type);
        }

        if (expression instanceof Id<?> || expression instanceof In<?> || expression instanceof Out<?>
                || expression instanceof GenericRecursiveFunction<?> || expression instanceof CataPlan<?>) {
            Func<?, ?> function = requireFunction(type, expression);
            if (!compatible(function.input(), function.output())) {
                throw new IllegalArgumentException(expression + " requires an endomorphic function type, got " + type);
            }
        } else if (expression instanceof Bang<?>) {
            Func<?, ?> function = requireFunction(type, expression);
            if (!compatible(function.output(), Types.UNIT)) {
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
        return retypeAs(source.type(), replacement);
    }

    static <A> PointFree<A> retypeAs(Type<?> sourceType, PointFree<A> replacement) {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(replacement, "replacement");
        if (!compatible(sourceType, replacement.type())) {
            return replacement.retagUnsafe(cast(sourceType));
        }
        return replacement;
    }

    static Type<?> pairCompositionType(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        return compositionType(outer, inner);
    }

    static Type<?> pairCompositionType(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        return compositionType(context, outer, inner);
    }

    static Func<?, ?> requireFunction(Type<?> type, PointFree<?> expression) {
        if (type instanceof Func<?, ?> function) {
            return function;
        }
        throw new IllegalArgumentException(expression + " requires a function type, got " + type);
    }

    @SuppressWarnings("unchecked")
    private static <A> Type<A> cast(Type<?> type) {
        return (Type<A>) type;
    }
}

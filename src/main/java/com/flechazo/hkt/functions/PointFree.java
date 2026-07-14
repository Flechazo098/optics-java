package com.flechazo.hkt.functions;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public sealed interface PointFree<A> permits
        Value,
        Id,
        Bang,
        Fn,
        Comp,
        AppExpr,
        OpticTransformer,
        OpticApp,
        FoldQuery,
        In,
        Out,
        GenericRecursiveFunction,
        CataPlan,
        TypedPointFree,
        UnsafeTypedPointFree {
    A eval();

    Type<A> type();

    default PointFree<A> withType(Type<A> type) {
        Objects.requireNonNull(type, "type");
        return new TypedPointFree<>(this, PointFreeTypes.validateExplicit(this, type));
    }

    default PointFree<A> withType(TypeToken<?> type) {
        Objects.requireNonNull(type, "type");
        return withType(castType(Types.witness(type)));
    }

    default PointFree<A> retagUnsafe(Type<A> type) {
        Objects.requireNonNull(type, "type");
        return new UnsafeTypedPointFree<>(this, type);
    }

    default PointFree<A> retagUnsafe(TypeToken<?> type) {
        Objects.requireNonNull(type, "type");
        return retagUnsafe(castType(Types.witness(type)));
    }

    @SuppressWarnings("unchecked")
    default RewriteResult<A> all(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        switch (this) {
            case TypedPointFree<?> typed -> {
                PointFree<Object> expression = castExpression(typed.expression());
                PointFree<Object> next = rule.rewriteOrSame(expression);
                return next != expression
                        ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(typed, next))
                        : RewriteResult.unchanged(this);
            }
            case UnsafeTypedPointFree<?> typed -> {
                PointFree<Object> expression = castExpression(typed.expression());
                PointFree<Object> next = rule.rewriteOrSame(expression);
                return next != expression
                        ? RewriteResult.changed((PointFree<A>) next.retagUnsafe(castType(typed.expressionType())))
                        : RewriteResult.unchanged(this);
            }
            case Comp<?, ?> comp -> {
                List<PointFree<? extends Function<?, ?>>> functions = comp.functions();
                ArrayList<PointFree<? extends Function<?, ?>>> rewritten = null;
                for (int i = 0; i < functions.size(); i++) {
                    PointFree<? extends Function<?, ?>> function = functions.get(i);
                    PointFree<? extends Function<?, ?>> next = rule.rewriteOrSame(function);
                    if (next != function && rewritten == null) {
                        rewritten = new ArrayList<>(functions.size());
                        for (int j = 0; j < i; j++) {
                            rewritten.add(functions.get(j));
                        }
                    }
                    if (rewritten != null) {
                        rewritten.add(next);
                    }
                }
                return rewritten != null
                        ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(this, new Comp<>(rewritten)))
                        : RewriteResult.unchanged(this);
            }
            case AppExpr<?, ?> app -> {
                PointFree<Function<Object, Object>> function = castExpression(app.function());
                PointFree<Object> argument = castExpression(app.argument());
                PointFree<Function<Object, Object>> nextFunction = rule.rewriteOrSame(function);
                PointFree<Object> nextArgument = rule.rewriteOrSame(argument);
                return nextFunction != function || nextArgument != argument
                        ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(
                        this,
                        new AppExpr<>(nextFunction, nextArgument)))
                        : RewriteResult.unchanged(this);
            }
            case OpticApp<?, ?, ?, ?> opticApp -> {
                PointFree<Function<Object, Object>> function = castExpression(opticApp.function());
                PointFree<Function<Object, Object>> nextFunction = rule.rewriteOrSame(function);
                return nextFunction != function
                        ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(
                        this,
                        new OpticApp<>(castOptic(opticApp.optic()), nextFunction)))
                        : RewriteResult.unchanged(this);
            }
            default -> {
            }
        }
        return RewriteResult.unchanged(this);
    }

    @SuppressWarnings("unchecked")
    default RewriteResult<A> one(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        switch (this) {
            case TypedPointFree<?> typed -> {
                RewriteResult<Object> next = rule.rewrite(castExpression(typed.expression()));
                return next.changed()
                        ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(typed, next.expression()))
                        : RewriteResult.unchanged(this);
            }
            case UnsafeTypedPointFree<?> typed -> {
                RewriteResult<Object> next = rule.rewrite(castExpression(typed.expression()));
                return next.changed()
                        ? RewriteResult.changed((PointFree<A>) next.expression().retagUnsafe(castType(typed.expressionType())))
                        : RewriteResult.unchanged(this);
            }
            case Comp<?, ?> comp -> {
                List<PointFree<? extends Function<?, ?>>> functions = comp.functions();
                for (int i = 0; i < functions.size(); i++) {
                    RewriteResult<? extends Function<?, ?>> next =
                            rule.rewrite(castExpression(functions.get(i)));
                    if (next.changed()) {
                        ArrayList<PointFree<? extends Function<?, ?>>> rewritten = new ArrayList<>(functions);
                        rewritten.set(i, next.expression());
                        return RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(this, new Comp<>(rewritten)));
                    }
                }
                return RewriteResult.unchanged(this);
            }
            case AppExpr<?, ?> app -> {
                PointFree<Function<Object, Object>> function = castExpression(app.function());
                RewriteResult<Function<Object, Object>> nextFunction = rule.rewrite(function);
                if (nextFunction.changed()) {
                    return RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(
                            this,
                            new AppExpr<>(nextFunction.expression(), castExpression(app.argument()))));
                }
                RewriteResult<Object> nextArgument = rule.rewrite(castExpression(app.argument()));
                return nextArgument.changed()
                        ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(
                                this,
                                new AppExpr<>(function, nextArgument.expression())))
                        : RewriteResult.unchanged(this);
            }
            case OpticApp<?, ?, ?, ?> opticApp -> {
                PointFree<Function<Object, Object>> function = castExpression(opticApp.function());
                RewriteResult<Function<Object, Object>> nextFunction = rule.rewrite(function);
                return nextFunction.changed()
                        ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(
                                this,
                                new OpticApp<>(castOptic(opticApp.optic()), nextFunction.expression())))
                        : RewriteResult.unchanged(this);
            }
            default -> {
            }
        }
        return RewriteResult.unchanged(this);
    }

    static <A> PointFree<A> value(A value) {
        return new Value<>(value, Types.variable("Value"));
    }

    static <A> PointFree<A> value(A value, Type<A> type) {
        return new Value<>(value, type);
    }

    static <A> PointFree<A> value(A value, TypeToken<A> type) {
        return value(value, Types.witness(type));
    }

    static <A> PointFree<Function<A, A>> id() {
        return new Id<>(Types.variable("Id"));
    }

    static <A> PointFree<Function<A, A>> id(Type<A> type) {
        return new Id<>(type);
    }

    static <A> PointFree<Function<A, A>> id(TypeToken<A> type) {
        return id(Types.witness(type));
    }

    static <A> PointFree<Function<A, Unit>> bang() {
        return new Bang<>(Types.variable("Bang"));
    }

    static <A> PointFree<Function<A, Unit>> bang(Type<A> sourceType) {
        return new Bang<>(sourceType);
    }

    static <A> PointFree<Function<A, Unit>> bang(TypeToken<A> sourceType) {
        return bang(Types.witness(sourceType));
    }

    static <A, B> PointFree<Function<A, B>> fn(String name, Function<? super A, ? extends B> function) {
        return new Fn<>(name, function, Types.variable(name + ".in"), Types.variable(name + ".out"));
    }

    static <A, B> PointFree<Function<A, B>> fn(
            String name,
            Function<? super A, ? extends B> function,
            Type<A> argumentType,
            Type<B> resultType) {
        return new Fn<>(name, function, argumentType, resultType);
    }

    static <A, B> PointFree<Function<A, B>> fn(
            String name,
            Function<? super A, ? extends B> function,
            TypeToken<A> argumentType,
            TypeToken<B> resultType) {
        return fn(name, function, Types.witness(argumentType), Types.witness(resultType));
    }

    static <A, B> PointFree<B> app(PointFree<Function<A, B>> function, PointFree<A> argument) {
        return new AppExpr<>(function, argument);
    }

    static <S, A> PointFree<Function<S, S>> lensApp(
            LensPath<S, A> path, PointFree<Function<A, A>> function) {
        return new OpticApp<>(PointFreeOptic.lens(path), function);
    }

    static <S, T, A, B> PointFree<Function<S, T>> opticApp(
            PointFreeOptic<S, T, A, B> optic, PointFree<? extends Function<?, ?>> function) {
        return new OpticApp<>(optic, function);
    }

    static <S, T, A, B> PointFree<Function<Function<A, B>, Function<S, T>>> opticTransformer(
            PointFreeOptic<S, T, A, B> optic) {
        return new OpticTransformer<>(optic);
    }

    static <A> PointFree<Function<A, A>> in(RecursiveFamily family, int index) {
        return new In<>(family, index, Types.variable(family.name() + "#" + index));
    }

    static <A> PointFree<Function<A, A>> in(RecursiveFamily family, int index, Type<A> type) {
        return new In<>(family, index, type);
    }

    static <A> PointFree<Function<A, A>> in(RecursiveFamily family, int index, TypeToken<A> type) {
        return in(family, index, Types.witness(type));
    }

    static <A> PointFree<Function<A, A>> out(RecursiveFamily family, int index) {
        return new Out<>(family, index, Types.variable(family.name() + "#" + index));
    }

    static <A> PointFree<Function<A, A>> out(RecursiveFamily family, int index, Type<A> type) {
        return new Out<>(family, index, type);
    }

    static <A> PointFree<Function<A, A>> out(RecursiveFamily family, int index, TypeToken<A> type) {
        return out(family, index, Types.witness(type));
    }

    static <A extends RecursiveTerm<A>> PointFree<Function<A, A>> genericRecursive(
            String name,
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra) {
        return GenericRecursiveFunction.of(name, family, index, algebra);
    }

    static <A extends RecursiveTerm<A>> PointFree<Function<A, A>> genericRecursive(
            String name,
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Type<A> recursiveType) {
        return GenericRecursiveFunction.of(name, family, index, algebra, recursiveType);
    }

    static <A extends RecursiveTerm<A>> PointFree<Function<A, A>> genericRecursive(
            String name,
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            TypeToken<A> recursiveType) {
        return GenericRecursiveFunction.of(name, family, index, algebra, recursiveType);
    }

    static <A, B> PointFree<Function<Tuple2<A, B>, Tuple2<A, B>>> productFirst(
            PointFree<Function<A, A>> function) {
        return new OpticApp<>(PointFreeOptic.product(ProductSide.FIRST), function);
    }

    static <A, B> PointFree<Function<Tuple2<A, B>, Tuple2<A, B>>> productSecond(
            PointFree<Function<B, B>> function) {
        return new OpticApp<>(PointFreeOptic.product(ProductSide.SECOND), function);
    }

    static <L, R> PointFree<Function<Either<L, R>, Either<L, R>>> sumLeft(
            PointFree<Function<L, L>> function) {
        return new OpticApp<>(PointFreeOptic.sum(SumSide.LEFT), function);
    }

    static <L, R> PointFree<Function<Either<L, R>, Either<L, R>>> sumRight(
            PointFree<Function<R, R>> function) {
        return new OpticApp<>(PointFreeOptic.sum(SumSide.RIGHT), function);
    }

    static <A, B, C> PointFree<Function<A, C>> comp(
            PointFree<? extends Function<B, C>> first,
            PointFree<? extends Function<A, B>> second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first instanceof Id<?>) {
            return castExpression(second);
        }
        if (second instanceof Id<?>) {
            return castExpression(first);
        }

        int firstSize = first instanceof Comp<?, ?> fc ? fc.functions().size() : 1;
        int secondSize = second instanceof Comp<?, ?> sc ? sc.functions().size() : 1;
        ArrayList<PointFree<? extends Function<?, ?>>> functions = new ArrayList<>(firstSize + secondSize);
        addCompFunctions(functions, first);
        addCompFunctions(functions, second);
        return new Comp<>(functions);
    }

    @SuppressWarnings("unchecked")
    private static <A> PointFree<A> castExpression(PointFree<?> value) {
        return (PointFree<A>) value;
    }

    @SuppressWarnings("unchecked")
    private static void addCompFunctions(
            List<PointFree<? extends Function<?, ?>>> functions,
            PointFree<?> function) {
        if (function instanceof Comp<?, ?> comp) {
            functions.addAll(comp.functions());
        } else {
            functions.add((PointFree<? extends Function<?, ?>>) function);
        }
    }

    @SuppressWarnings("unchecked")
    private static <S, T, A, B> PointFreeOptic<S, T, A, B> castOptic(PointFreeOptic<?, ?, ?, ?> value) {
        return (PointFreeOptic<S, T, A, B>) value;
    }

    @SuppressWarnings("unchecked")
    private static <A> Type<A> castType(Type<?> type) {
        return (Type<A>) type;
    }
}

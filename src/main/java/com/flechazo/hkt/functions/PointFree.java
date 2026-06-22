package com.flechazo.hkt.functions;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;

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
        CataPlan,
        TypedPointFree,
        UnsafeTypedPointFree {
    A eval();

    default Maybe<TypeExpr> type() {
        return Maybe.none();
    }

    default PointFree<A> withType(TypeExpr type) {
        Objects.requireNonNull(type, "type");
        return new TypedPointFree<>(this, PointFreeTypes.validateExplicit(this, type));
    }

    default PointFree<A> withType(TypeRef<?> type) {
        Objects.requireNonNull(type, "type");
        return withType(type.expr());
    }

    default PointFree<A> retagUnsafe(TypeExpr type) {
        Objects.requireNonNull(type, "type");
        return new UnsafeTypedPointFree<>(this, type);
    }

    default PointFree<A> retagUnsafe(TypeRef<?> type) {
        Objects.requireNonNull(type, "type");
        return retagUnsafe(type.expr());
    }

    @SuppressWarnings("unchecked")
    default Maybe<PointFree<A>> all(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        switch (this) {
            case TypedPointFree<?> typed -> {
                PointFree<Object> expression = cast(typed.expression());
                PointFree<Object> next = rule.rewriteOrSame(expression);
                return next != expression
                        ? Maybe.some((PointFree<A>) PointFreeTypes.retypeLike(typed, next))
                        : Maybe.none();
            }
            case UnsafeTypedPointFree<?> typed -> {
                PointFree<Object> expression = cast(typed.expression());
                PointFree<Object> next = rule.rewriteOrSame(expression);
                return next != expression
                        ? Maybe.some((PointFree<A>) next.retagUnsafe(typed.expressionType()))
                        : Maybe.none();
            }
            case Comp<?, ?> comp -> {
                ArrayList<PointFree<? extends Function<?, ?>>> functions = new ArrayList<>(comp.functions().size());
                boolean rewritten = false;
                for (PointFree<? extends Function<?, ?>> function : comp.functions()) {
                    PointFree<? extends Function<?, ?>> next = rule.rewriteOrSame(function);
                    rewritten |= next != function;
                    functions.add(next);
                }
                return rewritten
                        ? Maybe.some((PointFree<A>) PointFreeTypes.retypeLike(this, new Comp<>(functions)))
                        : Maybe.none();
            }
            case AppExpr<?, ?> app -> {
                PointFree<Function<Object, Object>> function = cast(app.function());
                PointFree<Object> argument = cast(app.argument());
                PointFree<Function<Object, Object>> nextFunction = rule.rewriteOrSame(function);
                PointFree<Object> nextArgument = rule.rewriteOrSame(argument);
                return nextFunction != function || nextArgument != argument
                        ? Maybe.some((PointFree<A>) PointFreeTypes.retypeLike(
                                this,
                                new AppExpr<>(nextFunction, nextArgument)))
                        : Maybe.none();
            }
            case OpticApp<?, ?, ?, ?> opticApp -> {
                PointFree<Function<Object, Object>> function = cast(opticApp.function());
                PointFree<Function<Object, Object>> nextFunction = rule.rewriteOrSame(function);
                return nextFunction != function
                        ? Maybe.some((PointFree<A>) PointFreeTypes.retypeLike(
                                this,
                                new OpticApp<>(narrow(opticApp.optic()), nextFunction)))
                        : Maybe.none();
            }
            default -> {
            }
        }
        return Maybe.none();
    }

    @SuppressWarnings("unchecked")
    default Maybe<PointFree<A>> one(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        switch (this) {
            case TypedPointFree<?> typed -> {
                return rule.rewrite(cast(typed.expression()))
                        .map(next -> (PointFree<A>) PointFreeTypes.retypeLike(typed, next));
            }
            case UnsafeTypedPointFree<?> typed -> {
                return rule.rewrite(cast(typed.expression()))
                        .map(next -> (PointFree<A>) next.retagUnsafe(typed.expressionType()));
            }
            case Comp<?, ?> comp -> {
                List<PointFree<? extends Function<?, ?>>> functions = comp.functions();
                for (int i = 0; i < functions.size(); i++) {
                    Maybe<PointFree<? extends Function<?, ?>>> next = narrow(rule.rewrite(cast(functions.get(i))));
                    if (next.isDefined()) {
                        ArrayList<PointFree<? extends Function<?, ?>>> rewritten = new ArrayList<>(functions);
                        rewritten.set(i, next.get());
                        return Maybe.some((PointFree<A>) PointFreeTypes.retypeLike(this, new Comp<>(rewritten)));
                    }
                }
                return Maybe.none();
            }
            case AppExpr<?, ?> app -> {
                PointFree<Function<Object, Object>> function = cast(app.function());
                Maybe<PointFree<Function<Object, Object>>> nextFunction = rule.rewrite(function);
                if (nextFunction.isDefined()) {
                    return Maybe.some((PointFree<A>) PointFreeTypes.retypeLike(
                            this,
                            new AppExpr<>(nextFunction.get(), cast(app.argument()))));
                }
                Maybe<PointFree<Object>> nextArgument = rule.rewrite(cast(app.argument()));
                return nextArgument.map(argument -> (PointFree<A>) PointFreeTypes.retypeLike(
                        this,
                        new AppExpr<>(function, argument)));
            }
            case OpticApp<?, ?, ?, ?> opticApp -> {
                PointFree<Function<Object, Object>> function = cast(opticApp.function());
                Maybe<PointFree<Function<Object, Object>>> nextFunction = rule.rewrite(function);
                return nextFunction.map(next -> (PointFree<A>) PointFreeTypes.retypeLike(
                        this,
                        new OpticApp<>(narrow(opticApp.optic()), next)));
            }
            default -> {
            }
        }
        return Maybe.none();
    }

    static <A> PointFree<A> value(A value) {
        return new Value<>(value);
    }

    static <A> PointFree<A> value(A value, TypeExpr type) {
        return value(value).withType(type);
    }

    static <A> PointFree<A> value(A value, TypeRef<A> type) {
        return value(value, type.expr());
    }

    static <A> PointFree<Function<A, A>> id() {
        return new Id<>();
    }

    static <A> PointFree<Function<A, A>> id(TypeExpr type) {
        return cast(PointFree.<A>id().withType(TypeExpr.function(type, type)));
    }

    static <A> PointFree<Function<A, A>> id(TypeRef<A> type) {
        return id(type.expr());
    }

    static <A> PointFree<Function<A, Unit>> bang() {
        return new Bang<>();
    }

    static <A> PointFree<Function<A, Unit>> bang(TypeExpr sourceType) {
        return cast(PointFree.<A>bang().withType(TypeExpr.function(sourceType, TypeRef.of(Unit.class).expr())));
    }

    static <A> PointFree<Function<A, Unit>> bang(TypeRef<A> sourceType) {
        return bang(sourceType.expr());
    }

    static <A, B> PointFree<Function<A, B>> fn(String name, Function<? super A, ? extends B> function) {
        return new Fn<>(name, function);
    }

    static <A, B> PointFree<Function<A, B>> fn(
            String name,
            Function<? super A, ? extends B> function,
            TypeExpr argumentType,
            TypeExpr resultType) {
        PointFree<Function<A, B>> fn = fn(name, function);
        return cast(fn.withType(TypeExpr.function(argumentType, resultType)));
    }

    static <A, B> PointFree<Function<A, B>> fn(
            String name,
            Function<? super A, ? extends B> function,
            TypeRef<A> argumentType,
            TypeRef<B> resultType) {
        return fn(name, function, argumentType.expr(), resultType.expr());
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
        return new In<>(family, index);
    }

    static <A> PointFree<Function<A, A>> in(RecursiveFamily family, int index, TypeExpr type) {
        return cast(PointFree.<A>in(family, index).withType(TypeExpr.function(type, type)));
    }

    static <A> PointFree<Function<A, A>> in(RecursiveFamily family, int index, TypeRef<A> type) {
        return in(family, index, type.expr());
    }

    static <A> PointFree<Function<A, A>> out(RecursiveFamily family, int index) {
        return new Out<>(family, index);
    }

    static <A> PointFree<Function<A, A>> out(RecursiveFamily family, int index, TypeExpr type) {
        return cast(PointFree.<A>out(family, index).withType(TypeExpr.function(type, type)));
    }

    static <A> PointFree<Function<A, A>> out(RecursiveFamily family, int index, TypeRef<A> type) {
        return out(family, index, type.expr());
    }

    static <A, B> PointFree<Function<Pair<A, B>, Pair<A, B>>> productFirst(
            PointFree<Function<A, A>> function) {
        return new OpticApp<>(PointFreeOptic.product(ProductSide.FIRST), function);
    }

    static <A, B> PointFree<Function<Pair<A, B>, Pair<A, B>>> productSecond(
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
            return cast(second);
        }
        if (second instanceof Id<?>) {
            return cast(first);
        }

        ArrayList<PointFree<? extends Function<?, ?>>> functions = new ArrayList<>();
        addCompFunctions(functions, first);
        addCompFunctions(functions, second);
        return new Comp<>(functions);
    }

    @SuppressWarnings("unchecked")
    private static <A> PointFree<A> cast(PointFree<?> value) {
        return (PointFree<A>) value;
    }

    @SuppressWarnings("unchecked")
    private static void addCompFunctions(
            List<PointFree<? extends Function<?, ?>>> functions, PointFree<?> function) {
        if (function instanceof Comp<?, ?>(List<PointFree<? extends Function<?, ?>>> functions1)) {
            functions.addAll(functions1);
        } else {
            functions.add((PointFree<? extends Function<?, ?>>) function);
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> A narrow(Object value) {
        return (A) value;
    }
}

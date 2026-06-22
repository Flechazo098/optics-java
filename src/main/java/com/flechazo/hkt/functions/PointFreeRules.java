package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.TypeRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("DeconstructionCanBeUsed")
public final class PointFreeRules {
    private PointFreeRules() {
    }

    public static PointFreeRule basic() {
        return PointFreeRule.choice(
                appNest(),
                bangEta(),
                compFlatten(),
                compId(),
                opticAppId(),
                compRewrite(
                        PointFreeRules::rewriteSameOpticPair,
                        PointFreeRules::rewriteOpticPrefixPair,
                        PointFreeRules::rewriteProductOrderPair,
                        PointFreeRules::rewriteSumOrderPair,
                        PointFreeRules::rewriteInOutPair,
                        PointFreeRules::rewriteSameCataPair,
                        PointFreeRules::rewriteDifferentCataPair));
    }

    public static PointFreeRule compFlatten() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return asCompFunctions(expression).flatMap(functions1 -> {
                    ArrayList<PointFree<? extends Function<?, ?>>> functions = new ArrayList<>();
                    boolean flattened = false;
                    for (PointFree<? extends Function<?, ?>> function : functions1) {
                        if (function instanceof Comp<?, ?>(List<PointFree<? extends Function<?, ?>>> functions2)) {
                            functions.addAll(functions2);
                            flattened = true;
                        } else {
                            functions.add(function);
                        }
                    }
                    return flattened
                            ? Maybe.some(cast(PointFreeTypes.retypeLike(expression, new Comp<>(functions))))
                            : Maybe.none();
                });
            }
        };
    }

    public static PointFreeRule compId() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return asCompFunctions(expression).flatMap(functions1 -> {
                    ArrayList<PointFree<? extends Function<?, ?>>> functions = new ArrayList<>();
                    boolean removed = false;
                    for (PointFree<? extends Function<?, ?>> function : functions1) {
                        if (function instanceof Id<?>) {
                            removed = true;
                        } else {
                            functions.add(function);
                        }
                    }
                    if (!removed) {
                        return Maybe.none();
                    }
                    if (functions.isEmpty()) {
                        return Maybe.some(cast(PointFreeTypes.retypeLike(expression, PointFree.id())));
                    }
                    if (functions.size() == 1) {
                        return Maybe.some(cast(PointFreeTypes.retypeLike(expression, functions.getFirst())));
                    }
                    return Maybe.some(cast(PointFreeTypes.retypeLike(expression, new Comp<>(functions))));
                });
            }
        };
    }

    public static PointFreeRule bangComp() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                if (!(expression instanceof Comp<?, ?>(List<PointFree<? extends Function<?, ?>>> functions))
                        || !(functions.getFirst() instanceof Bang<?>)) {
                    return Maybe.none();
                }
                return Maybe.some(cast(PointFreeTypes.retypeLike(expression, PointFree.bang())));
            }
        };
    }

    public static PointFreeRule bangEta() {
        return PointFreeRule.choice(appBang(), bangComp());
    }

    public static PointFreeRule lensAppId() {
        return opticAppId();
    }

    public static PointFreeRule productAppId() {
        return opticAppId();
    }

    public static PointFreeRule sumAppId() {
        return opticAppId();
    }

    public static PointFreeRule opticAppId() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                if (expression instanceof OpticApp<?, ?, ?, ?> opticApp
                        && opticApp.function() instanceof Id<?>) {
                    Maybe<PointFreeOpticTypes> opticTypes = opticApp.optic().types();
                    if (opticTypes.isDefined()
                            && !PointFreeTypes.compatible(opticTypes.get().source(), opticTypes.get().target())) {
                        return Maybe.none();
                    }
                    return Maybe.some(cast(PointFreeTypes.retypeLike(expression, PointFree.id())));
                }
                return Maybe.none();
            }
        };
    }

    public static PointFreeRule appNest() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                if (!(expression instanceof AppExpr<?, ?> outer)
                        || !(outer.argument() instanceof AppExpr<?, ?> inner)) {
                    return Maybe.none();
                }
                PointFree<Function<Object, Object>> function =
                        PointFree.comp(cast(outer.function()), cast(inner.function()));
                return Maybe.some(cast(PointFreeTypes.retypeLike(expression, PointFree.app(function, cast(inner.argument())))));
            }
        };
    }

    public static PointFreeRule appBang() {
        return new PointFreeRule() {
            @SuppressWarnings("RedundantCast")
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                if (expression instanceof AppExpr<?, ?> app
                        && (Object) app.function() instanceof Bang<?>) {
                    return Maybe.some(cast(PointFreeTypes.retypeLike(
                            expression,
                            PointFree.value(Unit.INSTANCE, TypeRef.of(Unit.class)))));
                }
                return Maybe.none();
            }
        };
    }

    public static PointFreeRule lensComp() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteSameOpticPair);
            }
        };
    }

    public static PointFreeRule lensPrefixFactor() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteOpticPrefixPair);
            }
        };
    }

    public static PointFreeRule productComp() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteSameOpticPair);
            }
        };
    }

    public static PointFreeRule sumComp() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteSameOpticPair);
            }
        };
    }

    public static PointFreeRule sortProj() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteProductOrderPair);
            }
        };
    }

    public static PointFreeRule sortInj() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteSumOrderPair);
            }
        };
    }

    public static PointFreeRule inOut() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteInOutPair);
            }
        };
    }

    public static PointFreeRule cataFuseSame() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteSameCataPair);
            }
        };
    }

    public static PointFreeRule cataFuseDifferent() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, PointFreeRules::rewriteDifferentCataPair);
            }
        };
    }

    private static PointFreeRule compRewrite(PairRewrite... rewrites) {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return rewriteComp(expression, rewrites);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <A> Maybe<PointFree<A>> rewriteComp(
            PointFree<A> expression,
            PairRewrite... rewrites) {
        if (!(expression instanceof Comp<?, ?>(List<PointFree<? extends Function<?, ?>>> functions))) {
            return Maybe.none();
        }
        Deque<PointFree<? extends Function<?, ?>>> result = new ArrayDeque<>(functions.size());
        Deque<PointFree<? extends Function<?, ?>>> queue = new ArrayDeque<>(functions);
        boolean rewritten = false;

        while (!queue.isEmpty()) {
            PointFree<? extends Function<?, ?>> next = queue.removeFirst();
            PointFree<? extends Function<?, ?>> last = result.peekLast();
            Maybe<PointFree<? extends Function<?, ?>>> replacement =
                    last == null ? Maybe.none() : rewriteFirst(rewrites, last, next);
            if (replacement.isDefined()) {
                result.removeLast();
                addFirst(queue, replacement.get());
                rewritten = true;
            } else {
                result.addLast(next);
            }
        }

        return rewritten ? Maybe.some((PointFree<A>) PointFreeTypes.retypeLike(expression, compact(result))) : Maybe.none();
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteFirst(
            PairRewrite[] rewrites,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        for (PairRewrite rewrite : rewrites) {
            Maybe<PointFree<? extends Function<?, ?>>> replacement = rewrite.rewrite(outer, inner);
            if (replacement.isDefined()) {
                return Maybe.some(PointFreeTypes.retypeAs(
                        PointFreeTypes.pairCompositionType(outer, inner),
                        replacement.get()));
            }
        }
        return Maybe.none();
    }

    private static void addFirst(
            Deque<PointFree<? extends Function<?, ?>>> queue,
            PointFree<? extends Function<?, ?>> function) {
        if (function instanceof Comp<?, ?>(List<PointFree<? extends Function<?, ?>>> functions)) {
            for (int i = functions.size() - 1; i >= 0; i--) {
                queue.addFirst(functions.get(i));
            }
        } else {
            queue.addFirst(function);
        }
    }

    private static PointFree<? extends Function<?, ?>> compact(
            Deque<PointFree<? extends Function<?, ?>>> functions) {
        if (functions.size() == 1) {
            return functions.getFirst();
        }
        return new Comp<>(List.copyOf(functions));
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteSameOpticPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)
                || !outerApp.optic().sameElements(innerApp.optic())) {
            return Maybe.none();
        }
        PointFree<Function<Object, Object>> function =
                PointFree.comp(cast(outerApp.function()), cast(innerApp.function()));
        return Maybe.some(PointFree.opticApp(cast(outerApp.optic()), function));
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteOpticPrefixPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)) {
            return Maybe.none();
        }

        PointFreeOptic<Object, Object, Object, Object> outerOptic = cast(outerApp.optic());
        PointFreeOptic<Object, Object, Object, Object> innerOptic = cast(innerApp.optic());
        int prefixSize = outerOptic.commonPrefixLength(innerOptic);
        if (prefixSize == 0
                || prefixSize == outerOptic.size() && prefixSize == innerOptic.size()) {
            return Maybe.none();
        }

        PointFreeOptic<Object, Object, Object, Object> prefix = outerOptic.prefix(prefixSize);
        PointFreeOptic<Object, Object, Object, Object> outerSuffix = outerOptic.suffix(prefixSize);
        PointFreeOptic<Object, Object, Object, Object> innerSuffix = innerOptic.suffix(prefixSize);
        PointFree<Function<Object, Object>> nested =
                PointFree.comp(
                        PointFree.opticApp(outerSuffix, cast(outerApp.function())),
                        PointFree.opticApp(innerSuffix, cast(innerApp.function())));
        return Maybe.some(PointFree.opticApp(prefix, nested));
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteProductOrderPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)
                || !outerApp.optic().startsWith(PointFreeOpticKind.PRODUCT)
                || !innerApp.optic().startsWith(PointFreeOpticKind.PRODUCT)
                || !(outerApp.optic().outermost().untyped() instanceof ProductOpticElement outerProduct)
                || !(innerApp.optic().outermost().untyped() instanceof ProductOpticElement innerProduct)
                || sideRank(outerProduct.side()) <= sideRank(innerProduct.side())) {
            return Maybe.none();
        }
        return Maybe.some(PointFree.comp(cast(inner), cast(outer)));
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteSumOrderPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)
                || !outerApp.optic().startsWith(PointFreeOpticKind.SUM)
                || !innerApp.optic().startsWith(PointFreeOpticKind.SUM)
                || !(outerApp.optic().outermost().untyped() instanceof SumOpticElement outerSum)
                || !(innerApp.optic().outermost().untyped() instanceof SumOpticElement innerSum)
                || sideRank(outerSum.side()) <= sideRank(innerSum.side())) {
            return Maybe.none();
        }
        return Maybe.some(PointFree.comp(cast(inner), cast(outer)));
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteInOutPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (outer instanceof In<?> in && inner instanceof Out<?> out
                && in.family().equals(out.family()) && in.index() == out.index()) {
            return Maybe.some(PointFree.id());
        }
        if (outer instanceof Out<?> out && inner instanceof In<?> in
                && out.family().equals(in.family()) && out.index() == in.index()) {
            return Maybe.some(PointFree.id());
        }
        return Maybe.none();
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteCataFuse(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner,
            BiFunction<CataPlan<Object>, CataPlan<Object>, Maybe<CataPlan<Object>>> fusor) {
        if (!(outer instanceof CataPlan<?> outerCata) || !(inner instanceof CataPlan<?> innerCata)) {
            return Maybe.none();
        }
        CataPlan<Object> outerPlan = cast(outerCata);
        CataPlan<Object> innerPlan = cast(innerCata);
        return fusor.apply(outerPlan, innerPlan).map(plan -> plan);
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteSameCataPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        return rewriteCataFuse(outer, inner, CataPlan::fuseSame);
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteDifferentCataPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        return rewriteCataFuse(outer, inner, CataPlan::fuseDifferent);
    }

    private static int sideRank(ProductSide side) {
        return switch (side) {
            case FIRST -> 0;
            case SECOND -> 1;
        };
    }

    private static int sideRank(SumSide side) {
        return switch (side) {
            case LEFT -> 0;
            case RIGHT -> 1;
        };
    }

    @FunctionalInterface
    private interface PairRewrite {
        Maybe<PointFree<? extends Function<?, ?>>> rewrite(
                PointFree<? extends Function<?, ?>> outer,
                PointFree<? extends Function<?, ?>> inner);
    }

    private static Maybe<List<PointFree<? extends Function<?, ?>>>> asCompFunctions(PointFree<?> expression) {
        if (!(expression instanceof Comp<?, ?> comp)) {
            return Maybe.none();
        }
        return Maybe.some(comp.functions());
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }
}

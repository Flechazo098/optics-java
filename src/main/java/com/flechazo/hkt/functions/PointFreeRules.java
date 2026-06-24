package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.Func;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;

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
                        if (function instanceof Comp<?, ?> comp) {
                            functions.addAll(comp.functions());
                            flattened = true;
                        } else {
                            functions.add(function);
                        }
                    }
                    return flattened
                            ? Maybe.some(narrow(PointFreeTypes.retypeLike(expression, new Comp<>(functions))))
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
                        return Maybe.some(narrow(PointFreeTypes.retypeLike(expression, PointFree.id())));
                    }
                    if (functions.size() == 1) {
                        return Maybe.some(narrow(PointFreeTypes.retypeLike(expression, functions.getFirst())));
                    }
                    return Maybe.some(narrow(PointFreeTypes.retypeLike(expression, new Comp<>(functions))));
                });
            }
        };
    }

    public static PointFreeRule bangComp() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                if (!(expression instanceof Comp<?, ?> comp)
                        || !(comp.functions().getFirst() instanceof Bang<?>)) {
                    return Maybe.none();
                }
                return Maybe.some(narrow(PointFreeTypes.retypeLike(expression, PointFree.bang())));
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
                    PointFreeOpticTypes<?, ?, ?, ?> opticTypes = opticApp.optic().types();
                    if (!PointFreeTypes.compatible(opticTypes.source(), opticTypes.target())) {
                        return Maybe.none();
                    }
                    return Maybe.some(narrow(PointFreeTypes.retypeLike(
                            expression,
                            PointFree.id(castType(opticTypes.source())))));
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
                        PointFree.comp(narrow(outer.function()), narrow(inner.function()));
                return Maybe.some(narrow(PointFreeTypes.retypeLike(expression, PointFree.app(function, narrow(inner.argument())))));
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
                    return Maybe.some(narrow(PointFreeTypes.retypeLike(
                            expression,
                            PointFree.value(Unit.INSTANCE, TypeToken.of(Unit.class)))));
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
        if (!(expression instanceof Comp<?, ?> comp)) {
            return Maybe.none();
        }
        Deque<PointFree<? extends Function<?, ?>>> result = new ArrayDeque<>(comp.functions().size());
        Deque<PointFree<? extends Function<?, ?>>> queue = new ArrayDeque<>(comp.functions());
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
        if (function instanceof Comp<?, ?> comp) {
            for (int i = comp.functions().size() - 1; i >= 0; i--) {
                queue.addFirst(comp.functions().get(i));
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
        return new Comp<>(new ArrayList<>(functions));
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
                PointFree.comp(narrow(outerApp.function()), narrow(innerApp.function()));
        return Maybe.some(PointFree.opticApp(narrow(outerApp.optic()), function));
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteOpticPrefixPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)) {
            return Maybe.none();
        }

        PointFreeOptic<Object, Object, Object, Object> outerOptic = narrow(outerApp.optic());
        PointFreeOptic<Object, Object, Object, Object> innerOptic = narrow(innerApp.optic());
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
                        PointFree.opticApp(outerSuffix, narrow(outerApp.function())),
                        PointFree.opticApp(innerSuffix, narrow(innerApp.function())));
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
        return Maybe.some(sortIndependentOpticApps(outerApp, innerApp));
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
        return Maybe.some(sortIndependentOpticApps(outerApp, innerApp));
    }

    private static Maybe<PointFree<? extends Function<?, ?>>> rewriteInOutPair(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (outer instanceof In<?> in && inner instanceof Out<?> out
                && in.family().equals(out.family()) && in.index() == out.index()) {
            return Maybe.some(PointFree.id(castType(in.recursiveType())));
        }
        if (outer instanceof Out<?> out && inner instanceof In<?> in
                && out.family().equals(in.family()) && out.index() == in.index()) {
            return Maybe.some(PointFree.id(castType(out.recursiveType())));
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
        CataPlan<Object> outerPlan = narrow(outerCata);
        CataPlan<Object> innerPlan = narrow(innerCata);
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

    private static PointFree<? extends Function<?, ?>> sortIndependentOpticApps(
            OpticApp<?, ?, ?, ?> outerApp,
            OpticApp<?, ?, ?, ?> innerApp) {
        Func<?, ?> outerFunctionType = PointFreeTypes.functionType(outerApp);
        Func<?, ?> innerFunctionType = PointFreeTypes.functionType(innerApp);
        PointFreeOptic<Object, Object, Object, Object> oldOuter = narrow(outerApp.optic());
        PointFreeOptic<Object, Object, Object, Object> oldInner = narrow(innerApp.optic());

        // Once independent optics are swapped, each optic keeps its original focus
        // A/B, but its outer S/T must describe the new execution boundary:
        // source -> other-branch-updated intermediate -> final target.
        Type<Object> source = castType(innerFunctionType.input());
        Type<Object> intermediate = sortIntermediateType(oldInner, oldOuter);
        Type<Object> target = castType(outerFunctionType.output());

        PointFreeOptic<Object, Object, Object, Object> sortedInner =
                oldOuter.castOuter(source, intermediate);
        PointFreeOptic<Object, Object, Object, Object> sortedOuter =
                oldInner.castOuter(intermediate, target);
        return PointFree.comp(
                PointFree.opticApp(sortedOuter, narrow(innerApp.function())),
                PointFree.opticApp(sortedInner, narrow(outerApp.function())));
    }

    private static Type<Object> sortIntermediateType(
            PointFreeOptic<Object, Object, Object, Object> lowRank,
            PointFreeOptic<Object, Object, Object, Object> highRank) {
        TypedOptic.Element<?, ?, ?, ?> low = lowRank.typed().elements().getFirst();
        TypedOptic.Element<?, ?, ?, ?> high = highRank.typed().elements().getFirst();
        if (low.optic() instanceof ProductOpticElement lowProduct
                && lowProduct.side() == ProductSide.FIRST
                && high.optic() instanceof ProductOpticElement highProduct
                && highProduct.side() == ProductSide.SECOND) {
            return castType(Types.and(castType(low.aType()), castType(high.bType())));
        }
        if (low.optic() instanceof SumOpticElement lowSum
                && lowSum.side() == SumSide.LEFT
                && high.optic() instanceof SumOpticElement highSum
                && highSum.side() == SumSide.RIGHT) {
            return castType(Types.or(castType(low.aType()), castType(high.bType())));
        }
        throw new IllegalArgumentException("Unsupported independent optic sort: " + low.optic() + " before " + high.optic());
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
    private static <A> A narrow(Object value) {
        return (A) value;
    }

    @SuppressWarnings("unchecked")
    private static <A> Type<A> castType(Type<?> type) {
        return (Type<A>) type;
    }
}

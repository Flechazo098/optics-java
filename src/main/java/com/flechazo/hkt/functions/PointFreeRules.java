package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.Func;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("DeconstructionCanBeUsed")
public final class PointFreeRules {
    private static final PointFreeRule COMP_FLATTEN = new PointFreeRule() {
        // f ◦ (g ◦ h) -> f ◦ g ◦ h
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (!(expression instanceof Comp<?, ?> source)) {
                return RewriteResult.unchanged(expression);
            }
            ArrayList<PointFree<? extends Function<?, ?>>> functions = new ArrayList<>(source.functions().size());
            boolean flattened = false;
            for (PointFree<? extends Function<?, ?>> function : source.functions()) {
                if (function instanceof Comp<?, ?> comp) {
                    functions.addAll(comp.functions());
                    flattened = true;
                } else {
                    functions.add(function);
                }
            }
            return flattened
                    ? RewriteResult.changed(narrow(PointFreeTypes.retypeLike(expression, new Comp<>(functions))))
                    : RewriteResult.unchanged(expression);
        }
    };

    private static final PointFreeRule COMP_ID = new PointFreeRule() {
        // f ◦ id -> f, id ◦ f -> f
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (!(expression instanceof Comp<?, ?> source)) {
                return RewriteResult.unchanged(expression);
            }
            ArrayList<PointFree<? extends Function<?, ?>>> functions = new ArrayList<>(source.functions().size());
            boolean removed = false;
            for (PointFree<? extends Function<?, ?>> function : source.functions()) {
                if (function instanceof Id<?>) {
                    removed = true;
                } else {
                    functions.add(function);
                }
            }
            if (!removed) {
                return RewriteResult.unchanged(expression);
            }
            if (functions.isEmpty()) {
                return RewriteResult.changed(narrow(PointFreeTypes.retypeLike(expression, PointFree.id())));
            }
            if (functions.size() == 1) {
                return RewriteResult.changed(narrow(PointFreeTypes.retypeLike(expression, functions.getFirst())));
            }
            return RewriteResult.changed(narrow(PointFreeTypes.retypeLike(expression, new Comp<>(functions))));
        }
    };

    private static final PointFreeRule BANG_COMP = new PointFreeRule() {
        // ! ◦ f -> !
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (!(expression instanceof Comp<?, ?> comp)
                    || !(comp.functions().getFirst() instanceof Bang<?>)) {
                return RewriteResult.unchanged(expression);
            }
            return RewriteResult.changed(narrow(PointFreeTypes.retypeLike(expression, PointFree.bang())));
        }
    };

    private static final PointFreeRule APP_BANG = new PointFreeRule() {
        // (ap ! x) -> ()
        @SuppressWarnings("RedundantCast")
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (expression instanceof AppExpr<?, ?> app
                    && (Object) app.function() instanceof Bang<?>) {
                return RewriteResult.changed(narrow(PointFreeTypes.retypeLike(
                        expression,
                        PointFree.value(Unit.INSTANCE, TypeToken.of(Unit.class)))));
            }
            return RewriteResult.unchanged(expression);
        }
    };

    private static final PointFreeRule BANG_ETA = PointFreeRule.choice(APP_BANG, BANG_COMP);

    private static final PointFreeRule OPTIC_APP_ID = new PointFreeRule() {
        // (ap lens id) -> id
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (expression instanceof OpticApp<?, ?, ?, ?> opticApp
                    && opticApp.function() instanceof Id<?>) {
                PointFreeOpticTypes<?, ?, ?, ?> opticTypes = opticApp.optic().types();
                if (!PointFreeTypes.compatible(opticTypes.source(), opticTypes.target())) {
                    return RewriteResult.unchanged(expression);
                }
                return RewriteResult.changed(narrow(PointFreeTypes.retypeLike(
                        expression,
                        PointFree.id(castType(opticTypes.source())))));
            }
            return RewriteResult.unchanged(expression);
        }
    };

    private static final PointFreeRule APP_NEST = new PointFreeRule() {
        // (ap f1 (ap f2 arg)) -> (ap (f1 ◦ f2) arg)
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (!(expression instanceof AppExpr<?, ?> outer)
                    || !(outer.argument() instanceof AppExpr<?, ?> inner)) {
                return RewriteResult.unchanged(expression);
            }
            PointFree<Function<Object, Object>> function =
                    PointFree.comp(narrow(outer.function()), narrow(inner.function()));
            return RewriteResult.changed(narrow(PointFreeTypes.retypeLike(
                    expression,
                    PointFree.app(function, narrow(inner.argument())))));
        }
    };

    private static final PointFreeRule APP_ID = new PointFreeRule() {
        // (ap id x) -> x
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (!(expression instanceof AppExpr<?, ?> app)
                    || !((Object) app.function() instanceof Id<?>)) {
                return RewriteResult.unchanged(expression);
            }
            return RewriteResult.changed(PointFreeTypes.retypeLike(expression, narrow(app.argument())));
        }
    };

    private static final PointFreeRule APP_FN_VALUE = new PointFreeRule() {
        // (ap f value) -> value'
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (!(expression instanceof AppExpr<?, ?> app)
                    || !((Object) app.function() instanceof Fn<?, ?> function)
                    || !(app.argument() instanceof Value<?> argument)) {
                return RewriteResult.unchanged(expression);
            }
            Object result = applyFunction(function, argument.value());
            return RewriteResult.changed(narrow(PointFree.value(result, castType(expression.type()))));
        }
    };

    private static final PointFreeRule FUNCTION_UNIT_ETA = new PointFreeRule() {
        // f : A -> Unit -> !
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (expression instanceof Bang<?>) {
                return RewriteResult.unchanged(expression);
            }
            Func<?, ?> functionType;
            try {
                functionType = context.functionType(expression);
            } catch (IllegalArgumentException ignored) {
                return RewriteResult.unchanged(expression);
            }
            if (!Types.UNIT.equals(functionType.output())) {
                return RewriteResult.unchanged(expression);
            }
            return RewriteResult.changed(narrow(PointFree.bang(castType(functionType.input()))));
        }
    };

    private static final PointFreeRule GENERIC_RECURSIVE_SPECIALIZATION = new PointFreeRule() {
        // genericRecursive algebra -> cata algebra
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (!(expression instanceof GenericRecursiveFunction<?> generic)) {
                return RewriteResult.unchanged(expression);
            }
            return RewriteResult.changed(narrow(generic.specialize()));
        }
    };

    private static final PointFreeRule REFLEX_CATA = new PointFreeRule() {
        // (|inµF|)µF -> idµF
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            if (!(expression instanceof CataPlan<?> cata)
                    || !cata.isReflexiveIdentityCata()) {
                return RewriteResult.unchanged(expression);
            }
            return RewriteResult.changed(narrow(PointFreeTypes.retypeLike(
                    expression,
                    PointFree.id(castType(cata.planRewrite().sourceType())))));
        }
    };

    private static final PointFreeRule LENS_COMP = new PointFreeRule() {
        // (ap lens f)◦(ap lens g) -> (ap lens (f ◦ g))
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            return rewriteComp(context, expression, PointFreeRules::rewriteSameOpticPair);
        }
    };

    private static final PointFreeRule LENS_PREFIX_FACTOR = new PointFreeRule() {
        // (ap (p ◦ o1) f) ◦ (ap (p ◦ o2) g) -> (ap p ((ap o1 f) ◦ (ap o2 g)))
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            return rewriteComp(context, expression, PointFreeRules::rewriteOpticPrefixPair);
        }
    };

    private static final PointFreeRule PRODUCT_COMP = LENS_COMP;
    private static final PointFreeRule SUM_COMP = LENS_COMP;

    private static final PointFreeRule SORT_PROJ = new PointFreeRule() {
        // (ap π1 f)◦(ap π2 g) -> (ap π2 g)◦(ap π1 f)
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            return rewriteComp(context, expression, PointFreeRules::rewriteProductOrderPair);
        }
    };

    private static final PointFreeRule SORT_INJ = new PointFreeRule() {
        // (ap i1 f)◦(ap i2 g) -> (ap i2 g)◦(ap i1 f)
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            return rewriteComp(context, expression, PointFreeRules::rewriteSumOrderPair);
        }
    };

    private static final PointFreeRule IN_OUT = new PointFreeRule() {
        // in ◦ out -> id, out ◦ in -> id
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            return rewriteComp(context, expression, PointFreeRules::rewriteInOutPair);
        }
    };

    private static final PointFreeRule CATA_FUSE_SAME = new PointFreeRule() {
        // (fold g ◦ in) ◦ fold (f ◦ in) -> fold ( g ◦ f ◦ in), <== g ◦ in ◦ fold (f ◦ in) ◦ out == in ◦ fold (f ◦ in) ◦ out ◦ g <== g doesn't touch fold's index
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            return rewriteComp(context, expression, PointFreeRules::rewriteSameCataPair);
        }
    };

    private static final PointFreeRule CATA_FUSE_DIFFERENT = new PointFreeRule() {
        // (fold g ◦ in) ◦ fold (f ◦ in) -> fold ( g ◦ f ◦ in), <== g ◦ in ◦ fold (f ◦ in) ◦ out == in ◦ fold (f ◦ in) ◦ out ◦ g <== g doesn't touch fold's index
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            return rewriteComp(context, expression, PointFreeRules::rewriteDifferentCataPair);
        }
    };

    private static final PairRewrite[] BASIC_COMP_REWRITES = {
            PointFreeRules::rewriteOpticAppPair,
            PointFreeRules::rewriteInOutPair,
            PointFreeRules::rewriteSameCataPair,
            PointFreeRules::rewriteDifferentCataPair,
            PointFreeRules::rewriteOpticTransformerPair,
            PointFreeRules::rewriteOpaqueFnPair
    };

    private static final PointFreeRule BASIC_COMP_REWRITE = compRewrite(BASIC_COMP_REWRITES);

    private static final PointFreeRule[] APP_RULES = {
            FUNCTION_UNIT_ETA,
            APP_ID,
            APP_FN_VALUE,
            APP_NEST,
            APP_BANG
    };

    private static final PointFreeRule[] COMP_RULES = {
            FUNCTION_UNIT_ETA,
            BANG_COMP,
            COMP_FLATTEN,
            COMP_ID,
            BASIC_COMP_REWRITE
    };

    private static final PointFreeRule[] CATA_RULES = {
            REFLEX_CATA,
            FUNCTION_UNIT_ETA
    };

    private static final PointFreeRule[] OPTIC_APP_RULES = {
            FUNCTION_UNIT_ETA,
            OPTIC_APP_ID
    };

    private static final PointFreeRule BASIC = new PointFreeRule() {
        @Override
        public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
            return switch (expression) {
                case GenericRecursiveFunction<?> ignored -> GENERIC_RECURSIVE_SPECIALIZATION.rewrite(context, expression);
                case CataPlan<?> ignored -> rewriteFirstChanged(context, expression, CATA_RULES);
                case AppExpr<?, ?> ignored -> rewriteFirstChanged(context, expression, APP_RULES);
                case Comp<?, ?> ignored -> rewriteFirstChanged(context, expression, COMP_RULES);
                case OpticApp<?, ?, ?, ?> ignored -> rewriteFirstChanged(context, expression, OPTIC_APP_RULES);
                default -> FUNCTION_UNIT_ETA.rewrite(context, expression);
            };
        }
    };

    private PointFreeRules() {
    }

    public static PointFreeRule basic() {
        return BASIC;
    }

    public static PointFreeRule compFlatten() {
        return COMP_FLATTEN;
    }

    public static PointFreeRule compId() {
        return COMP_ID;
    }

    public static PointFreeRule bangComp() {
        return BANG_COMP;
    }

    public static PointFreeRule bangEta() {
        return BANG_ETA;
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
        return OPTIC_APP_ID;
    }

    public static PointFreeRule appNest() {
        return APP_NEST;
    }

    public static PointFreeRule appId() {
        return APP_ID;
    }

    public static PointFreeRule appFnValue() {
        return APP_FN_VALUE;
    }

    public static PointFreeRule appBang() {
        return APP_BANG;
    }

    public static PointFreeRule functionUnitEta() {
        return FUNCTION_UNIT_ETA;
    }

    public static PointFreeRule lensComp() {
        return LENS_COMP;
    }

    public static PointFreeRule lensPrefixFactor() {
        return LENS_PREFIX_FACTOR;
    }

    public static PointFreeRule productComp() {
        return PRODUCT_COMP;
    }

    public static PointFreeRule sumComp() {
        return SUM_COMP;
    }

    public static PointFreeRule sortProj() {
        return SORT_PROJ;
    }

    public static PointFreeRule sortInj() {
        return SORT_INJ;
    }

    public static PointFreeRule inOut() {
        return IN_OUT;
    }

    public static PointFreeRule genericRecursiveSpecialization() {
        return GENERIC_RECURSIVE_SPECIALIZATION;
    }

    public static PointFreeRule reflexCata() {
        return REFLEX_CATA;
    }

    public static PointFreeRule cataFuseSame() {
        return CATA_FUSE_SAME;
    }

    public static PointFreeRule cataFuseDifferent() {
        return CATA_FUSE_DIFFERENT;
    }

    private static PointFreeRule compRewrite(PairRewrite... rewrites) {
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                return rewriteComp(context, expression, rewrites);
            }
        };
    }

    private static <A> RewriteResult<A> rewriteFirstChanged(
            RewriteContext context,
            PointFree<A> expression,
            PointFreeRule[] rules) {
        for (PointFreeRule rule : rules) {
            RewriteResult<A> result = rule.rewrite(context, expression);
            if (result.changed()) {
                return result;
            }
        }
        return RewriteResult.unchanged();
    }

    private static <A> RewriteResult<A> rewriteComp(
            RewriteContext context,
            PointFree<A> expression,
            PairRewrite... rewrites) {
        if (!(expression instanceof Comp<?, ?> comp)) {
            return RewriteResult.unchanged(expression);
        }
        if (comp.functions().size() <= 4) {
            RewriteResult<A> small = rewriteCompSmall(context, expression, comp, rewrites);
            if (small.changed()) {
                return small;
            }
            return small;
        }
        return rewriteCompArray(context, expression, comp, rewrites);
    }

    @SuppressWarnings("unchecked")
    private static <A> RewriteResult<A> rewriteCompSmall(
            RewriteContext context,
            PointFree<A> expression,
            Comp<?, ?> comp,
            PairRewrite[] rewrites) {
        List<PointFree<? extends Function<?, ?>>> functions = comp.functions();
        PointFree<? extends Function<?, ?>> p0 = null;
        PointFree<? extends Function<?, ?>> p1 = null;
        PointFree<? extends Function<?, ?>> p2 = null;
        PointFree<? extends Function<?, ?>> p3 = null;
        int pendingSize = 0;
        for (int i = functions.size() - 1; i >= 0; i--) {
            PointFree<? extends Function<?, ?>> function = functions.get(i);
            switch (pendingSize++) {
                case 0 -> p0 = function;
                case 1 -> p1 = function;
                case 2 -> p2 = function;
                case 3 -> p3 = function;
                default -> {
                    return rewriteCompArray(context, expression, comp, rewrites);
                }
            }
        }

        PointFree<? extends Function<?, ?>> r0 = null;
        PointFree<? extends Function<?, ?>> r1 = null;
        PointFree<? extends Function<?, ?>> r2 = null;
        PointFree<? extends Function<?, ?>> r3 = null;
        int resultSize = 0;
        boolean rewritten = false;

        while (pendingSize > 0) {
            PointFree<? extends Function<?, ?>> next;
            switch (--pendingSize) {
                case 0 -> {
                    next = p0;
                    p0 = null;
                }
                case 1 -> {
                    next = p1;
                    p1 = null;
                }
                case 2 -> {
                    next = p2;
                    p2 = null;
                }
                case 3 -> {
                    next = p3;
                    p3 = null;
                }
                default -> throw new IllegalStateException("small pending stack overflow");
            }

            PointFree<? extends Function<?, ?>> last = switch (resultSize) {
                case 0 -> null;
                case 1 -> r0;
                case 2 -> r1;
                case 3 -> r2;
                case 4 -> r3;
                default -> throw new IllegalStateException("small result stack overflow");
            };
            PointFree<? extends Function<?, ?>> replacement =
                    last == null ? null : rewriteFirst(context, rewrites, last, next);
            if (replacement != null) {
                switch (--resultSize) {
                    case 0 -> r0 = null;
                    case 1 -> r1 = null;
                    case 2 -> r2 = null;
                    case 3 -> r3 = null;
                    default -> throw new IllegalStateException("small result stack underflow");
                }
                if (replacement instanceof Comp<?, ?> replacementComp) {
                    List<PointFree<? extends Function<?, ?>>> replacementFunctions = replacementComp.functions();
                    if (pendingSize + replacementFunctions.size() > 4) {
                        return rewriteCompArray(context, expression, comp, rewrites);
                    }
                    for (int i = replacementFunctions.size() - 1; i >= 0; i--) {
                        PointFree<? extends Function<?, ?>> function = replacementFunctions.get(i);
                        switch (pendingSize++) {
                            case 0 -> p0 = function;
                            case 1 -> p1 = function;
                            case 2 -> p2 = function;
                            case 3 -> p3 = function;
                            default -> {
                                return rewriteCompArray(context, expression, comp, rewrites);
                            }
                        }
                    }
                } else {
                    if (pendingSize == 4) {
                        return rewriteCompArray(context, expression, comp, rewrites);
                    }
                    switch (pendingSize++) {
                        case 0 -> p0 = replacement;
                        case 1 -> p1 = replacement;
                        case 2 -> p2 = replacement;
                        case 3 -> p3 = replacement;
                        default -> {
                            return rewriteCompArray(context, expression, comp, rewrites);
                        }
                    }
                }
                rewritten = true;
            } else {
                if (resultSize == 4) {
                    return rewriteCompArray(context, expression, comp, rewrites);
                }
                switch (resultSize++) {
                    case 0 -> r0 = next;
                    case 1 -> r1 = next;
                    case 2 -> r2 = next;
                    case 3 -> r3 = next;
                    default -> {
                        return rewriteCompArray(context, expression, comp, rewrites);
                    }
                }
            }
        }

        return rewritten
                ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(
                expression,
                compactSmall(r0, r1, r2, r3, resultSize)))
                : RewriteResult.unchanged(expression);
    }

    @SuppressWarnings("unchecked")
    private static <A> RewriteResult<A> rewriteCompArray(
            RewriteContext context,
            PointFree<A> expression,
            Comp<?, ?> comp,
            PairRewrite[] rewrites) {
        List<PointFree<? extends Function<?, ?>>> functions = comp.functions();
        PointFree<? extends Function<?, ?>>[] pending = functionArray(functions.size());
        int pendingSize = 0;
        for (int i = functions.size() - 1; i >= 0; i--) {
            pending[pendingSize++] = functions.get(i);
        }
        PointFree<? extends Function<?, ?>>[] result = functionArray(functions.size());
        int resultSize = 0;
        boolean rewritten = false;

        while (pendingSize > 0) {
            PointFree<? extends Function<?, ?>> next = pending[--pendingSize];
            pending[pendingSize] = null;
            PointFree<? extends Function<?, ?>> last = resultSize == 0 ? null : result[resultSize - 1];
            PointFree<? extends Function<?, ?>> replacement =
                    last == null ? null : rewriteFirst(context, rewrites, last, next);
            if (replacement != null) {
                result[--resultSize] = null;
                if (replacement instanceof Comp<?, ?> replacementComp) {
                    List<PointFree<? extends Function<?, ?>>> replacementFunctions = replacementComp.functions();
                    pending = ensureCapacity(pending, pendingSize + replacementFunctions.size());
                    for (int i = replacementFunctions.size() - 1; i >= 0; i--) {
                        pending[pendingSize++] = replacementFunctions.get(i);
                    }
                } else {
                    pending = ensureCapacity(pending, pendingSize + 1);
                    pending[pendingSize++] = replacement;
                }
                rewritten = true;
            } else {
                result = ensureCapacity(result, resultSize + 1);
                result[resultSize++] = next;
            }
        }

        return rewritten
                ? RewriteResult.changed((PointFree<A>) PointFreeTypes.retypeLike(expression, compact(result, resultSize)))
                : RewriteResult.unchanged(expression);
    }

    private static PointFree<? extends Function<?, ?>> compactSmall(
            PointFree<? extends Function<?, ?>> f0,
            PointFree<? extends Function<?, ?>> f1,
            PointFree<? extends Function<?, ?>> f2,
            PointFree<? extends Function<?, ?>> f3,
            int size) {
        if (size == 1) {
            return f0;
        }
        ArrayList<PointFree<? extends Function<?, ?>>> result = new ArrayList<>(size);
        if (size > 0) {
            result.add(f0);
        }
        if (size > 1) {
            result.add(f1);
        }
        if (size > 2) {
            result.add(f2);
        }
        if (size > 3) {
            result.add(f3);
        }
        return new Comp<>(result);
    }

    private static PointFree<? extends Function<?, ?>> rewriteFirst(
            RewriteContext context,
            PairRewrite[] rewrites,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        for (PairRewrite rewrite : rewrites) {
            PointFree<? extends Function<?, ?>> replacement = rewrite.rewrite(context, outer, inner);
            if (replacement != null) {
                return PointFreeTypes.retypeAs(
                        PointFreeTypes.pairCompositionType(context, outer, inner),
                        replacement);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static PointFree<? extends Function<?, ?>>[] functionArray(int size) {
        return (PointFree<? extends Function<?, ?>>[]) new PointFree<?>[Math.max(4, size)];
    }

    private static PointFree<? extends Function<?, ?>>[] ensureCapacity(
            PointFree<? extends Function<?, ?>>[] values,
            int size) {
        return size <= values.length ? values : Arrays.copyOf(values, Math.max(size, values.length * 2));
    }

    private static PointFree<? extends Function<?, ?>> compact(
            PointFree<? extends Function<?, ?>>[] functions,
            int size) {
        if (size == 1) {
            return functions[0];
        }
        ArrayList<PointFree<? extends Function<?, ?>>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(functions[i]);
        }
        return new Comp<>(result);
    }

    private static PointFree<? extends Function<?, ?>> rewriteSameOpticPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)
                || !outerApp.optic().sameElements(innerApp.optic())) {
            return null;
        }
        PointFree<Function<Object, Object>> function =
                PointFree.comp(narrow(outerApp.function()), narrow(innerApp.function()));
        return PointFree.opticApp(narrow(outerApp.optic()), function);
    }

    private static PointFree<? extends Function<?, ?>> rewriteOpticAppPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)) {
            return null;
        }

        if (outerApp.optic().sameElements(innerApp.optic())) {
            PointFree<Function<Object, Object>> function =
                    PointFree.comp(narrow(outerApp.function()), narrow(innerApp.function()));
            return PointFree.opticApp(narrow(outerApp.optic()), function);
        }

        if (outerApp.optic().startsWith(PointFreeOpticKind.PRODUCT)
                && innerApp.optic().startsWith(PointFreeOpticKind.PRODUCT)
                && outerApp.optic().outermost().untyped() instanceof ProductOpticElement outerProduct
                && innerApp.optic().outermost().untyped() instanceof ProductOpticElement innerProduct
                && sideRank(outerProduct.side()) > sideRank(innerProduct.side())) {
            return sortIndependentOpticApps(context, outerApp, innerApp);
        }

        if (outerApp.optic().startsWith(PointFreeOpticKind.SUM)
                && innerApp.optic().startsWith(PointFreeOpticKind.SUM)
                && outerApp.optic().outermost().untyped() instanceof SumOpticElement outerSum
                && innerApp.optic().outermost().untyped() instanceof SumOpticElement innerSum
                && sideRank(outerSum.side()) > sideRank(innerSum.side())) {
            return sortIndependentOpticApps(context, outerApp, innerApp);
        }

        PointFreeOptic<Object, Object, Object, Object> outerOptic = narrow(outerApp.optic());
        PointFreeOptic<Object, Object, Object, Object> innerOptic = narrow(innerApp.optic());
        int prefixSize = outerOptic.commonPrefixLength(innerOptic);
        if (prefixSize > 0
                && (prefixSize != outerOptic.size() || prefixSize != innerOptic.size())) {
            PointFreeOptic<Object, Object, Object, Object> prefix = outerOptic.prefix(prefixSize);
            PointFreeOptic<Object, Object, Object, Object> outerSuffix = outerOptic.suffix(prefixSize);
            PointFreeOptic<Object, Object, Object, Object> innerSuffix = innerOptic.suffix(prefixSize);
            PointFree<Function<Object, Object>> nested =
                    PointFree.comp(
                            PointFree.opticApp(outerSuffix, narrow(outerApp.function())),
                            PointFree.opticApp(innerSuffix, narrow(innerApp.function())));
            return PointFree.opticApp(prefix, nested);
        }

        TypedOptic.Element<?, ?, ?, ?> outerElement = outerApp.optic().typed().elements().getFirst();
        TypedOptic.Element<?, ?, ?, ?> innerElement = innerApp.optic().typed().elements().getFirst();
        if (OpticIndependence.swapEvidence(outerElement, innerElement).isDefined()) {
            return sortIndependentOpticApps(context, outerApp, innerApp);
        }
        return null;
    }

    private static PointFree<? extends Function<?, ?>> rewriteOpticPrefixPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)) {
            return null;
        }

        PointFreeOptic<Object, Object, Object, Object> outerOptic = narrow(outerApp.optic());
        PointFreeOptic<Object, Object, Object, Object> innerOptic = narrow(innerApp.optic());
        int prefixSize = outerOptic.commonPrefixLength(innerOptic);
        if (prefixSize == 0
                || prefixSize == outerOptic.size() && prefixSize == innerOptic.size()) {
            return null;
        }

        PointFreeOptic<Object, Object, Object, Object> prefix = outerOptic.prefix(prefixSize);
        PointFreeOptic<Object, Object, Object, Object> outerSuffix = outerOptic.suffix(prefixSize);
        PointFreeOptic<Object, Object, Object, Object> innerSuffix = innerOptic.suffix(prefixSize);
        PointFree<Function<Object, Object>> nested =
                PointFree.comp(
                        PointFree.opticApp(outerSuffix, narrow(outerApp.function())),
                        PointFree.opticApp(innerSuffix, narrow(innerApp.function())));
        return PointFree.opticApp(prefix, nested);
    }

    private static PointFree<? extends Function<?, ?>> rewriteProductOrderPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)
                || !outerApp.optic().startsWith(PointFreeOpticKind.PRODUCT)
                || !innerApp.optic().startsWith(PointFreeOpticKind.PRODUCT)
                || !(outerApp.optic().outermost().untyped() instanceof ProductOpticElement outerProduct)
                || !(innerApp.optic().outermost().untyped() instanceof ProductOpticElement innerProduct)
                || sideRank(outerProduct.side()) <= sideRank(innerProduct.side())) {
            return null;
        }
        return sortIndependentOpticApps(context, outerApp, innerApp);
    }

    private static PointFree<? extends Function<?, ?>> rewriteSumOrderPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)
                || !outerApp.optic().startsWith(PointFreeOpticKind.SUM)
                || !innerApp.optic().startsWith(PointFreeOpticKind.SUM)
                || !(outerApp.optic().outermost().untyped() instanceof SumOpticElement outerSum)
                || !(innerApp.optic().outermost().untyped() instanceof SumOpticElement innerSum)
                || sideRank(outerSum.side()) <= sideRank(innerSum.side())) {
            return null;
        }
        return sortIndependentOpticApps(context, outerApp, innerApp);
    }

    private static PointFree<? extends Function<?, ?>> rewriteIndependentBranchOrderPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticApp<?, ?, ?, ?> outerApp)
                || !(inner instanceof OpticApp<?, ?, ?, ?> innerApp)) {
            return null;
        }
        TypedOptic.Element<?, ?, ?, ?> outerElement = outerApp.optic().typed().elements().getFirst();
        TypedOptic.Element<?, ?, ?, ?> innerElement = innerApp.optic().typed().elements().getFirst();
        if (OpticIndependence.swapEvidence(outerElement, innerElement).isEmpty()) {
            return null;
        }
        return sortIndependentOpticApps(context, outerApp, innerApp);
    }

    private static PointFree<? extends Function<?, ?>> rewriteInOutPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (outer instanceof In<?> in && inner instanceof Out<?> out
                && in.family().equals(out.family()) && in.index() == out.index()) {
            return PointFree.id(castType(in.recursiveType()));
        }
        if (outer instanceof Out<?> out && inner instanceof In<?> in
                && out.family().equals(in.family()) && out.index() == in.index()) {
            return PointFree.id(castType(out.recursiveType()));
        }
        return null;
    }

    private static PointFree<? extends Function<?, ?>> rewriteCataFuse(
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner,
            BiFunction<CataPlan<Object>, CataPlan<Object>, Maybe<CataPlan<Object>>> fusor) {
        if (!(outer instanceof CataPlan<?> outerCata) || !(inner instanceof CataPlan<?> innerCata)) {
            return null;
        }
        CataPlan<Object> outerPlan = narrow(outerCata);
        CataPlan<Object> innerPlan = narrow(innerCata);
        Maybe<CataPlan<Object>> result = fusor.apply(outerPlan, innerPlan);
        return result.isDefined() ? result.get() : null;
    }

    private static PointFree<? extends Function<?, ?>> rewriteSameCataPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        return rewriteCataFuse(outer, inner, CataPlan::fuseSame);
    }

    private static PointFree<? extends Function<?, ?>> rewriteDifferentCataPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        return rewriteCataFuse(outer, inner, CataPlan::fuseDifferent);
    }

    private static PointFree<? extends Function<?, ?>> rewriteOpticTransformerPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof OpticTransformer<?, ?, ?, ?> outerTransformer)
                || !(inner instanceof OpticTransformer<?, ?, ?, ?> innerTransformer)) {
            return null;
        }
        return composeOpticTransformers(outerTransformer, innerTransformer);
    }

    @SuppressWarnings("unchecked")
    private static <X, Y, S, T, A, B> PointFree<Function<Function<A, B>, Function<X, Y>>> composeOpticTransformers(
            OpticTransformer<?, ?, ?, ?> outer,
            OpticTransformer<?, ?, ?, ?> inner) {
        // Optic[o1] ◦ Optic[o2] -> Optic[o1 ◦ o2]
        PointFreeOptic<X, Y, S, T> outerOptic = (PointFreeOptic<X, Y, S, T>) outer.optic();
        PointFreeOptic<S, T, A, B> innerOptic = (PointFreeOptic<S, T, A, B>) inner.optic();
        return PointFree.opticTransformer(outerOptic.andThen(innerOptic));
    }

    private static PointFree<? extends Function<?, ?>> rewriteOpaqueFnPair(
            RewriteContext context,
            PointFree<? extends Function<?, ?>> outer,
            PointFree<? extends Function<?, ?>> inner) {
        if (!(outer instanceof Fn<?, ?> outerFn) || !(inner instanceof Fn<?, ?> innerFn)) {
            return null;
        }
        Func<?, ?> outerType = context.functionType(outer);
        Func<?, ?> innerType = context.functionType(inner);
        if (!context.compatible(innerType.output(), outerType.input())) {
            return null;
        }
        Func<?, ?> type = context.function(castType(innerType.input()), castType(outerType.output()));
        String name = outerFn.name() + " ◦ " + innerFn.name();
        PointFree<Function<Object, Object>> composed = PointFree.fn(
                name,
                value -> applyFunction(outerFn, applyFunction(innerFn, value)),
                castType(type.input()),
                castType(type.output()));
        return composed;
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
            RewriteContext context,
            OpticApp<?, ?, ?, ?> outerApp,
            OpticApp<?, ?, ?, ?> innerApp) {
        Func<?, ?> outerFunctionType = context.functionType(outerApp);
        Func<?, ?> innerFunctionType = context.functionType(innerApp);
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
        Maybe<OpticIndependence.SwapEvidence> evidence = OpticIndependence.swapEvidence(high, low);
        if (evidence.isDefined()) {
            return evidence.get().intermediateType(castType(low.sType()));
        }
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
        PointFree<? extends Function<?, ?>> rewrite(
                RewriteContext context,
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
    private static Object applyFunction(Fn<?, ?> function, Object value) {
        return ((Function<Object, Object>) function.eval()).apply(value);
    }

    @SuppressWarnings("unchecked")
    private static <A> Type<A> castType(Type<?> type) {
        return (Type<A>) type;
    }

    private static Func<?, ?> castFunc(Type<?> type) {
        return (Func<?, ?>) type;
    }
}

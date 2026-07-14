package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.Func;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class PointFreeNormalForm {
    private PointFreeNormalForm() {
    }

    public static boolean isNormal(PointFree<?> expression) {
        return firstViolation(expression).isEmpty();
    }

    public static Maybe<String> firstViolation(PointFree<?> expression) {
        Objects.requireNonNull(expression, "expression");
        return firstViolation(expression, "root");
    }

    @SuppressWarnings("RedundantCast")
    private static Maybe<String> firstViolation(PointFree<?> expression, String path) {
        if (expression instanceof TypedPointFree<?> typed) {
            return firstViolation(typed.expression(), path + ".typed");
        }
        if (expression instanceof UnsafeTypedPointFree<?> typed) {
            return firstViolation(typed.expression(), path + ".unsafeTyped");
        }
        if (expression instanceof Comp<?, ?> comp) {
            return compViolation(comp, path);
        }
        if (expression instanceof AppExpr<?, ?> app) {
            if (app.argument() instanceof AppExpr<?, ?>) {
                return Maybe.some(path + ": nested application argument must be lifted into function composition");
            }
            if ((Object) app.function() instanceof Id<?>) {
                return Maybe.some(path + ": identity application must be eliminated");
            }
            if ((Object) app.function() instanceof Fn<?, ?> && app.argument() instanceof Value<?>) {
                return Maybe.some(path + ": known function application to a known value must be constant-folded");
            }
            if ((Object) app.function() instanceof Bang<?>) {
                return Maybe.some(path + ": bang application must be constant-folded");
            }
            return firstViolation(app.function(), path + ".function")
                    .or(() -> firstViolation(app.argument(), path + ".argument"));
        }
        Maybe<String> unitEta = functionUnitEtaViolation(expression, path);
        if (unitEta.isDefined()) {
            return unitEta;
        }
        if (expression instanceof OpticApp<?, ?, ?, ?> opticApp) {
            if (opticApp.function() instanceof Id<?>
                    && PointFreeTypes.compatible(opticApp.optic().sourceType(), opticApp.optic().targetType())) {
                return Maybe.some(path + ": identity optic application must be eliminated");
            }
            return firstViolation(opticApp.function(), path + ".modifier");
        }
        if (expression instanceof GenericRecursiveFunction<?>) {
            return Maybe.some(path + ": generic recursive function must be specialized to a cata plan");
        }
        if (expression instanceof CataPlan<?> cata && cata.isReflexiveIdentityCata()) {
            return Maybe.some(path + ": reflexive identity cata must be eliminated");
        }
        return Maybe.none();
    }

    private static Maybe<String> compViolation(Comp<?, ?> comp, String path) {
        List<PointFree<? extends Function<?, ?>>> functions = comp.functions();
        if (functions.size() <= 1) {
            return Maybe.some(path + ": composition must be compacted");
        }
        if (functions.getFirst() instanceof Bang<?>) {
            return Maybe.some(path + ": composition with outer bang must collapse to bang");
        }

        for (int i = 0; i < functions.size(); i++) {
            PointFree<?> function = functions.get(i);
            if (function instanceof Comp<?, ?>) {
                return Maybe.some(path + "[" + i + "]: nested composition must be flattened");
            }
            if (function instanceof Id<?>) {
                return Maybe.some(path + "[" + i + "]: identity must be removed from composition");
            }
        }

        for (int i = 0; i < functions.size() - 1; i++) {
            Maybe<String> violation = pairViolation(functions.get(i), functions.get(i + 1), path + "[" + i + "," + (i + 1) + "]");
            if (violation.isDefined()) {
                return violation;
            }
        }

        for (int i = 0; i < functions.size(); i++) {
            Maybe<String> child = firstViolation(functions.get(i), path + "[" + i + "]");
            if (child.isDefined()) {
                return child;
            }
        }
        return Maybe.none();
    }

    private static Maybe<String> pairViolation(PointFree<?> outer, PointFree<?> inner, String path) {
        if (outer instanceof OpticApp<?, ?, ?, ?> outerApp
                && inner instanceof OpticApp<?, ?, ?, ?> innerApp) {
            return opticPairViolation(outerApp, innerApp, path);
        }
        if (outer instanceof OpticTransformer<?, ?, ?, ?> && inner instanceof OpticTransformer<?, ?, ?, ?>) {
            return Maybe.some(path + ": adjacent optic transformers must be composed");
        }
        if (outer instanceof Fn<?, ?> && inner instanceof Fn<?, ?>) {
            return Maybe.some(path + ": adjacent opaque functions must be compacted");
        }
        if (outer instanceof In<?> in && inner instanceof Out<?> out
                && in.family().equals(out.family()) && in.index() == out.index()) {
            return Maybe.some(path + ": matching recursive in/out boundary must be cancelled");
        }
        if (outer instanceof Out<?> out && inner instanceof In<?> in
                && out.family().equals(in.family()) && out.index() == in.index()) {
            return Maybe.some(path + ": matching recursive out/in boundary must be cancelled");
        }
        if (outer instanceof CataPlan<?> outerCata
                && inner instanceof CataPlan<?> innerCata
                && canFuseCata(outerCata, innerCata)) {
            return Maybe.some(path + ": fusable cata pair must be fused");
        }
        return Maybe.none();
    }

    private static Maybe<String> functionUnitEtaViolation(PointFree<?> expression, String path) {
        if (expression instanceof Bang<?>) {
            return Maybe.none();
        }
        Func<?, ?> functionType;
        try {
            functionType = PointFreeTypes.functionType(expression);
        } catch (IllegalArgumentException ignored) {
            return Maybe.none();
        }
        return com.flechazo.hkt.type.Types.UNIT.equals(functionType.output())
                ? Maybe.some(path + ": function returning Unit must be normalized to bang")
                : Maybe.none();
    }

    private static Maybe<String> opticPairViolation(
            OpticApp<?, ?, ?, ?> outer,
            OpticApp<?, ?, ?, ?> inner,
            String path) {
        if (outer.optic().sameElements(inner.optic())) {
            return Maybe.some(path + ": repeated optic application must be fused");
        }
        int commonPrefix = outer.optic().commonPrefixLength(inner.optic());
        if (commonPrefix > 0) {
            return Maybe.some(path + ": common optic prefix must be factored");
        }
        if (outer.optic().startsWith(PointFreeOpticKind.PRODUCT)
                && inner.optic().startsWith(PointFreeOpticKind.PRODUCT)
                && outer.optic().outermost().untyped() instanceof ProductOpticElement(ProductSide side3)
                && inner.optic().outermost().untyped() instanceof ProductOpticElement(ProductSide side2)
                && productRank(side3) > productRank(side2)) {
            return Maybe.some(path + ": product projections must be sorted by branch rank");
        }
        if (outer.optic().startsWith(PointFreeOpticKind.SUM)
                && inner.optic().startsWith(PointFreeOpticKind.SUM)
                && outer.optic().outermost().untyped() instanceof SumOpticElement(SumSide side1)
                && inner.optic().outermost().untyped() instanceof SumOpticElement(SumSide side)
                && sumRank(side1) > sumRank(side)) {
            return Maybe.some(path + ": sum injections must be sorted by branch rank");
        }
        TypedOptic.Element<?, ?, ?, ?> outerElement = outer.optic().typed().elements().getFirst();
        TypedOptic.Element<?, ?, ?, ?> innerElement = inner.optic().typed().elements().getFirst();
        if (OpticIndependence.swapEvidence(outerElement, innerElement).isDefined()) {
            return Maybe.some(path + ": independent optic branches must be sorted by evidence rank");
        }
        return Maybe.none();
    }

    private static int productRank(ProductSide side) {
        return switch (side) {
            case FIRST -> 0;
            case SECOND -> 1;
        };
    }

    private static int sumRank(SumSide side) {
        return switch (side) {
            case LEFT -> 0;
            case RIGHT -> 1;
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean canFuseCata(CataPlan<?> outer, CataPlan<?> inner) {
        CataPlan outerRaw = outer;
        CataPlan innerRaw = inner;
        return outerRaw.fuseSame(innerRaw).isDefined()
                || outerRaw.fuseDifferent(innerRaw).isDefined();
    }
}

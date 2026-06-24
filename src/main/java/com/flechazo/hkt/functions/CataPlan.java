package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class CataPlan<A> implements PointFree<Function<A, A>> {
    private final RecursiveFamily family;
    private final int index;
    private final AlgebraPlan algebra;
    private final AlgebraRewrite planRewrite;
    private final Type<A> recursiveType;

    private CataPlan(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            AlgebraRewrite planRewrite,
            Type<A> recursiveType) {
        this.family = Objects.requireNonNull(family, "family");
        family.checkIndex(index);
        this.index = index;
        this.algebra = Objects.requireNonNull(algebra, "algebra");
        if (!family.equals(algebra.family())) {
            throw new IllegalArgumentException("algebra family must match cata family");
        }
        this.recursiveType = Objects.requireNonNull(recursiveType, "recursiveType");
        this.planRewrite = Objects.requireNonNull(planRewrite, "planRewrite");
    }

    public static <A> CataPlan<A> of(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Function<A, A> evaluator) {
        return new CataPlan<>(
                family,
                index,
                algebra,
                AlgebraRewrite.rewrite(family.name() + "#" + index + ".cata", castFunction(evaluator)),
                Types.variable(family.name() + "#" + index));
    }

    public static <A> CataPlan<A> of(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Function<A, A> evaluator,
            Type<A> recursiveType) {
        return new CataPlan<>(
                family,
                index,
                algebra,
                AlgebraRewrite.rewrite(family.name() + "#" + index + ".cata", castFunction(evaluator)),
                recursiveType);
    }

    public static <A> CataPlan<A> of(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Function<A, A> evaluator,
            TypeToken<A> recursiveType) {
        return of(family, index, algebra, evaluator, Types.witness(recursiveType));
    }

    public static <A extends RecursiveTerm<A>> CataPlan<A> forTerms(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra) {
        return new CataPlan<>(
                family,
                index,
                algebra,
                termPlanRewrite(family, index, algebra),
                Types.variable(family.name() + "#" + index));
    }

    public static <A extends RecursiveTerm<A>> CataPlan<A> forTerms(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Type<A> recursiveType) {
        return new CataPlan<>(
                family,
                index,
                algebra,
                termPlanRewrite(family, index, algebra),
                recursiveType);
    }

    public static <A extends RecursiveTerm<A>> CataPlan<A> forTerms(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            TypeToken<A> recursiveType) {
        return forTerms(family, index, algebra, Types.witness(recursiveType));
    }

    @Override
    public Function<A, A> eval() {
        return castFunction(planRewrite.function());
    }

    @Override
    public Type<Function<A, A>> type() {
        return Types.function(recursiveType, recursiveType);
    }

    public RecursiveFamily family() {
        return family;
    }

    public int index() {
        return index;
    }

    public AlgebraPlan algebra() {
        return algebra;
    }

    public boolean isReflexiveIdentityCata() {
        return algebra.isReflexiveIdentityAlgebra()
                && planRewrite.isReflexiveIdentityAlgebra()
                && planRewrite.hasRecursiveDependencyEvidence()
                && PointFreeTypes.compatible(planRewrite.sourceType(), planRewrite.targetType());
    }

    Maybe<CataPlan<A>> fuseSame(CataPlan<A> inner) {
        Objects.requireNonNull(inner, "inner");
        if (!sameFoldBoundary(inner)) {
            return Maybe.none();
        }
        return algebra.fuseSame(inner.algebra).map(fused -> fusedWith(inner, fused));
    }

    Maybe<CataPlan<A>> fuseDifferent(CataPlan<A> inner) {
        Objects.requireNonNull(inner, "inner");
        if (!sameFoldBoundary(inner)) {
            return Maybe.none();
        }
        return algebra.fuseDifferent(inner.algebra).map(fused -> fusedWith(inner, fused));
    }

    private boolean sameFoldBoundary(CataPlan<A> inner) {
        return index == inner.index && family.equals(inner.family);
    }

    private CataPlan<A> fusedWith(CataPlan<A> inner, AlgebraPlan fusedAlgebra) {
        Type<A> fusedType = fusedType(inner);
        return new CataPlan<>(
                family,
                index,
                fusedAlgebra,
                planRewrite.compose(inner.planRewrite),
                fusedType);
    }

    private Type<A> fusedType(CataPlan<A> inner) {
        if (!PointFreeTypes.compatible(recursiveType, inner.recursiveType)) {
            throw new IllegalArgumentException("cata type mismatch: " + recursiveType + ", " + inner.recursiveType);
        }
        return recursiveType;
    }

    private static <A extends RecursiveTerm<A>> A rewriteTerm(
            RecursiveFamily family, AlgebraPlan algebra, A term) {
        Objects.requireNonNull(term, "term");
        int familyIndex = term.familyIndex();
        family.checkIndex(familyIndex);
        List<A> rewrittenChildren = term.children().stream()
                .map(child -> rewriteTerm(family, algebra, child))
                .toList();
        A rebuilt = term.withChildren(rewrittenChildren);
        return cast(algebra.branch(familyIndex).function().apply(rebuilt));
    }

    private static AlgebraRewrite termPlanRewrite(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra) {
        if (algebra.isReflexiveIdentityAlgebra()) {
            return AlgebraRewrite.reflexiveIdentityAlgebra(Types.variable(family.name() + "#" + index + ".terms.reflexive"));
        }
        return AlgebraRewrite.rewrite(
                family.name() + "#" + index + ".terms",
                value -> rewriteTermUnchecked(family, algebra, value));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object rewriteTermUnchecked(
            RecursiveFamily family,
            AlgebraPlan algebra,
            Object value) {
        return rewriteTerm(family, algebra, (RecursiveTerm) value);
    }

    public AlgebraRewrite planRewrite() {
        return planRewrite;
    }

    @Override
    public String toString() {
        return "cata(" + family.name() + "#" + index + ", " + algebra.name() + ")";
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }

    @SuppressWarnings("unchecked")
    private static <A, B> Function<A, B> castFunction(Function<?, ?> function) {
        return (Function<A, B>) function;
    }
}

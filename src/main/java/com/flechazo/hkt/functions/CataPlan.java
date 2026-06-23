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
    private final Function<A, A> evaluator;
    private final Type<A> recursiveType;

    private CataPlan(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Function<A, A> evaluator,
            Type<A> recursiveType) {
        this.family = Objects.requireNonNull(family, "family");
        family.checkIndex(index);
        this.index = index;
        this.algebra = Objects.requireNonNull(algebra, "algebra");
        if (!family.equals(algebra.family())) {
            throw new IllegalArgumentException("algebra family must match cata family");
        }
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.recursiveType = Objects.requireNonNull(recursiveType, "recursiveType");
    }

    public static <A> CataPlan<A> of(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Function<A, A> evaluator) {
        return new CataPlan<>(family, index, algebra, evaluator, Types.variable(family.name() + "#" + index));
    }

    public static <A> CataPlan<A> of(
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Function<A, A> evaluator,
            Type<A> recursiveType) {
        return new CataPlan<>(family, index, algebra, evaluator, recursiveType);
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
                term -> rewriteTerm(family, algebra, term),
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
                term -> rewriteTerm(family, algebra, term),
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
        return evaluator;
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
                value -> evaluator.apply(inner.evaluator.apply(value)),
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

    @Override
    public String toString() {
        return "cata(" + family.name() + "#" + index + ", " + algebra.name() + ")";
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }
}

package com.flechazo.hkt.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings("DeconstructionCanBeUsed")
public final class RewritePlan<S> implements Function<S, S> {
    private final List<LensStep<S, ?>> steps;

    private RewritePlan(List<LensStep<S, ?>> steps) {
        this.steps = steps;
    }

    public static <S> RewritePlan<S> identity() {
        return new RewritePlan<>(List.of());
    }

    public <A> RewritePlan<S> modify(LensPath<S, A> path, Function<? super A, ? extends A> f) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(f, "f");
        ArrayList<LensStep<S, ?>> next = new ArrayList<>(steps);
        next.add(new LensStep<>(path, f));
        return new RewritePlan<>(next);
    }

    public RewritePlan<S> andThen(RewritePlan<S> other) {
        Objects.requireNonNull(other, "other");
        ArrayList<LensStep<S, ?>> next = new ArrayList<>(steps);
        next.addAll(other.steps);
        return new RewritePlan<>(next);
    }

    public int size() {
        return steps.size();
    }

    public List<String> paths() {
        return steps.stream().map(step -> step.path().toString()).toList();
    }

    @Override
    public S apply(S source) {
        return toPointFree().eval().apply(source);
    }

    public RewritePlan<S> optimize() {
        return fromPointFree(PointFreeOptimizer.optimize(toPointFree()));
    }

    public PointFree<Function<S, S>> toPointFree() {
        PointFree<Function<S, S>> expression = PointFree.id();
        for (LensStep<S, ?> step : steps) {
            expression = PointFree.comp(step.toPointFree(), expression);
        }
        return expression;
    }

    public static <S> RewritePlan<S> fromPointFree(PointFree<Function<S, S>> expression) {
        Objects.requireNonNull(expression, "expression");
        ArrayList<LensStep<S, ?>> parsed = new ArrayList<>();
        if (appendSteps(expression, parsed)) {
            return new RewritePlan<>(parsed);
        }
        return RewritePlan.<S>identity().modify(LensPath.identity(), expression.eval());
    }

    @SuppressWarnings("unchecked")
    private static <S> boolean appendSteps(
            PointFree<? extends Function<?, ?>> expression, ArrayList<LensStep<S, ?>> steps) {
        if (expression instanceof Id<?>) {
            return true;
        }
        if (expression instanceof OpticApp<?, ?, ?, ?> opticApp && isLensOnly(opticApp.optic())) {
            steps.add(new LensStep<>(
                    (LensPath<S, Object>) lensPath(opticApp.optic()),
                    (Function<Object, Object>) opticApp.function().eval()));
            return true;
        }
        if (expression instanceof Comp<?, ?> comp) {
            ArrayList<LensStep<S, ?>> parsed = new ArrayList<>(comp.functions().size());
            List<PointFree<? extends Function<?, ?>>> functions = new ArrayList<>(comp.functions());
            Collections.reverse(functions);
            for (PointFree<? extends Function<?, ?>> function : functions) {
                if (!appendSteps(function, parsed)) {
                    return false;
                }
            }
            steps.addAll(parsed);
            return true;
        }
        return false;
    }

    private static boolean isLensOnly(PointFreeOptic<?, ?, ?, ?> optic) {
        return optic.containsOnly(PointFreeOpticKind.LENS);
    }

    private static LensPath<?, ?> lensPath(PointFreeOptic<?, ?, ?, ?> optic) {
        List<LensPath.Element> elements = optic.elements().stream()
                .map(LensOpticElement.class::cast)
                .map(LensOpticElement::element)
                .toList();
        return LensPath.fromElements(elements);
    }

    @Override
    public String toString() {
        return "RewritePlan" + paths();
    }

    private record LensStep<S, A>(LensPath<S, A> path, Function<? super A, ? extends A> function) {
        private LensStep {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(function, "function");
        }

        S apply(S source) {
            return path.modify(function, source);
        }

        PointFree<Function<S, S>> toPointFree() {
            Function<A, A> modifier = function::apply;
            return PointFree.lensApp(path, PointFree.fn(path.toString(), modifier));
        }
    }
}

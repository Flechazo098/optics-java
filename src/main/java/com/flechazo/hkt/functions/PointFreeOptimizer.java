package com.flechazo.hkt.functions;

import java.util.Objects;

public final class PointFreeOptimizer {
    private PointFreeOptimizer() {
    }

    public static PointFreeRule standardRule() {
        return PointFreeRule.bottomUpFix(PointFreeRules.basic());
    }

    public static <A> PointFree<A> optimize(PointFree<A> expression) {
        return optimize(expression, standardRule());
    }

    public static <A> PointFree<A> optimize(PointFree<A> expression, PointFreeRule rule) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(rule, "rule");
        return rule.rewriteOrSame(expression);
    }
}

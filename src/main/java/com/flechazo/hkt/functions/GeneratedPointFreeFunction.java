package com.flechazo.hkt.functions;

import java.util.Objects;
import java.util.function.Function;

abstract class GeneratedPointFreeFunction implements Function<Object, Object> {
    protected final Object[] constants;
    private final PointFree<?> optimizedPlan;
    private final Function<Object, Object> fallback;
    private final boolean interpretedFallback;

    protected GeneratedPointFreeFunction(
            Object[] constants,
            PointFree<?> optimizedPlan,
            Function<Object, Object> fallback,
            boolean interpretedFallback) {
        this.constants = Objects.requireNonNull(constants, "constants");
        this.optimizedPlan = Objects.requireNonNull(optimizedPlan, "optimizedPlan");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.interpretedFallback = interpretedFallback;
    }

    protected final Object fallbackApply(Object input) {
        return fallback.apply(input);
    }

    public final PointFree<?> optimizedPlan() {
        return optimizedPlan;
    }

    public final boolean usesInterpretedFallback() {
        return interpretedFallback;
    }
}

package com.flechazo.hkt.functions;

import java.util.Objects;

abstract class GeneratedPointFreeExecutor<A> implements PointFreeExecutor<A> {
    protected final Object[] constants;
    private final PointFree<A> optimizedPlan;
    private final boolean interpretedFallback;

    protected GeneratedPointFreeExecutor(
            Object[] constants,
            PointFree<A> optimizedPlan,
            boolean interpretedFallback) {
        this.constants = Objects.requireNonNull(constants, "constants");
        this.optimizedPlan = Objects.requireNonNull(optimizedPlan, "optimizedPlan");
        this.interpretedFallback = interpretedFallback;
    }

    @Override
    public final A execute() {
        return cast(executeRaw());
    }

    protected abstract Object executeRaw();

    protected final Object fallbackRaw() {
        return optimizedPlan.eval();
    }

    @Override
    public final PointFree<A> optimizedPlan() {
        return optimizedPlan;
    }

    @Override
    public final Class<?> executorClass() {
        return getClass();
    }

    @Override
    public final boolean usesInterpretedFallback() {
        return interpretedFallback;
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }
}

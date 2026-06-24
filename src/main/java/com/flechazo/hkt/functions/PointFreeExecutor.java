package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;

public interface PointFreeExecutor<A> {
    A execute();

    PointFree<A> optimizedPlan();

    Class<?> executorClass();

    boolean usesInterpretedFallback();

    default Type<A> type() {
        return optimizedPlan().type();
    }
}

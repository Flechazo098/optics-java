package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.Task;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SagaBuilder<A> {
    private final Saga<A> saga;
    private final int stepCount;

    private SagaBuilder(Saga<A> saga, int stepCount) {
        this.saga = Objects.requireNonNull(saga, "saga");
        this.stepCount = stepCount;
    }

    public static SagaBuilder<Unit> start() {
        return new SagaBuilder<>(new Saga<>(ignored -> Unit.INSTANCE), 0);
    }

    public <B> SagaBuilder<B> step(String name, Task<B> action, Consumer<? super B> compensate) {
        return step(name, ignored -> action, compensate);
    }

    public <B> SagaBuilder<B> step(String name, Function<? super A, Task<B>> action, Consumer<? super B> compensate) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(compensate, "compensate");
        return new SagaBuilder<>(
                saga.andThen(previous -> Saga.namedStepSync(name, action.apply(previous), compensate)),
                stepCount + 1);
    }

    public <B> SagaBuilder<B> stepAsync(String name, Function<? super A, Task<B>> action, Function<? super B, Task<Unit>> compensate) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(compensate, "compensate");
        return new SagaBuilder<>(
                saga.andThen(previous -> Saga.namedStep(name, action.apply(previous), compensate)),
                stepCount + 1);
    }

    public <B> SagaBuilder<B> stepNoCompensation(String name, Function<? super A, Task<B>> action) {
        return stepAsync(name, action, ignored -> Task.unit());
    }

    public Saga<A> build() {
        if (stepCount == 0) {
            throw new IllegalStateException("Saga must have at least one step");
        }
        return saga;
    }
}

package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.VTask;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public record SagaStep<A>(String name, VTask<A> action, Function<? super A, VTask<Unit>> compensate) {
    public SagaStep {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(compensate, "compensate");
    }

    public static <A> SagaStep<A> of(String name, VTask<A> action, Consumer<? super A> compensate) {
        Objects.requireNonNull(compensate, "compensate");
        return new SagaStep<>(name, action, value -> VTask.exec(() -> compensate.accept(value)));
    }

    public static <A> SagaStep<A> ofAsync(String name, VTask<A> action, Function<? super A, VTask<Unit>> compensate) {
        return new SagaStep<>(name, action, compensate);
    }
}

package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.Task;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public record SagaStep<A>(String name, Task<A> action, Function<? super A, Task<Unit>> compensate) {
    public SagaStep {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(compensate, "compensate");
    }

    public static <A> SagaStep<A> of(String name, Task<A> action, Consumer<? super A> compensate) {
        Objects.requireNonNull(compensate, "compensate");
        return new SagaStep<>(name, action, value -> Task.exec(() -> compensate.accept(value)));
    }

    public static <A> SagaStep<A> ofAsync(String name, Task<A> action, Function<? super A, Task<Unit>> compensate) {
        return new SagaStep<>(name, action, compensate);
    }
}

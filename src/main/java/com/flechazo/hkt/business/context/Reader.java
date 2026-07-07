package com.flechazo.hkt.business.context;

import com.flechazo.hkt.Unit;

import java.util.function.Function;

@FunctionalInterface
public interface Reader<R, A> {
    static <R, A> Reader<R, A> of(Function<? super R, ? extends A> run) {
        return run::apply;
    }

    static <R, A> Reader<R, A> constant(A value) {
        return ignored -> value;
    }

    static <R> Reader<R, R> ask() {
        return environment -> environment;
    }

    A run(R environment);

    default <B> Reader<R, B> map(Function<? super A, ? extends B> mapper) {
        return environment -> mapper.apply(run(environment));
    }

    default <B> Reader<R, B> flatMap(Function<? super A, ? extends Reader<R, B>> mapper) {
        return environment -> mapper.apply(run(environment)).run(environment);
    }

    default Reader<R, Unit> asUnit() {
        return map(ignored -> Unit.INSTANCE);
    }
}

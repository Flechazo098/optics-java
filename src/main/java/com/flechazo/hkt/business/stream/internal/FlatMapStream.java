package com.flechazo.hkt.business.stream.internal;

import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.stream.VStream;

import java.util.function.Function;

public final class FlatMapStream<A, B> implements VStream<B> {
    private final VStream<A> outer;
    private final Function<? super A, ? extends VStream<B>> mapper;

    public FlatMapStream(VStream<A> outer, Function<? super A, ? extends VStream<B>> mapper) {
        this.outer = outer;
        this.mapper = mapper;
    }

    @Override
    public Task<Step<B>> pull() {
        return outer.pull().flatMap(step -> switch (step) {
            case Emit<A> emit ->
                    Task.pure(new Skip<>(VStream.concat(mapper.apply(emit.value()), emit.tail().flatMap(mapper))));
            case Skip<A> skip -> Task.pure(new Skip<>(skip.tail().flatMap(mapper)));
            case Done<A> ignored -> Task.pure(new Done<>());
        });
    }
}

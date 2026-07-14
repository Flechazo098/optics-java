package com.flechazo.hkt.business.stream.internal;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.VTask;
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
    public VTask<Step<B>> pull() {
        return outer.pull().flatMap(step -> switch (step) {
            case Emit<A> emit ->
                    VTask.pure(new Skip<>(VStream.concat(mapper.apply(emit.value()), emit.tail().flatMap(mapper))));
            case Skip<A> skip -> VTask.pure(new Skip<>(skip.tail().flatMap(mapper)));
            case Done<A> ignored -> VTask.pure(new Done<>());
        });
    }

    @Override
    public VTask<Unit> close() {
        return outer.close();
    }
}

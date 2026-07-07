package com.flechazo.hkt.business.stream.internal;

import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.stream.VStream;

public final class InterleaveStream<A> implements VStream<A> {
    private final VStream<A> first;
    private final VStream<A> second;

    public InterleaveStream(VStream<A> first, VStream<A> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public Task<Step<A>> pull() {
        return first.pull().map(step -> switch (step) {
            case Emit<A> emit -> new Emit<>(emit.value(), new InterleaveStream<>(second, emit.tail()));
            case Skip<A> skip -> new Skip<>(new InterleaveStream<>(second, skip.tail()));
            case Done<A> ignored -> new Skip<>(second);
        });
    }
}

package com.flechazo.hkt.business.stream.internal;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.VTask;
import com.flechazo.hkt.business.stream.VStream;

import java.util.function.Function;

public final class MappedStream<A, B> implements VStream<B> {
    private final VStream<A> source;
    private final Function<? super A, ? extends B> mapper;

    public MappedStream(VStream<A> source, Function<? super A, ? extends B> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public VTask<Step<B>> pull() {
        return source.pull().map(step -> switch (step) {
            case Emit<A> emit -> new Emit<>(mapper.apply(emit.value()), emit.tail().map(mapper));
            case Skip<A> skip -> new Skip<>(skip.tail().map(mapper));
            case Done<A> ignored -> new Done<>();
        });
    }

    @Override
    public VTask<Unit> close() {
        return source.close();
    }

    @Override
    public <C> VStream<C> map(Function<? super B, ? extends C> f) {
        @SuppressWarnings("unchecked")
        Function<A, B> typedMapper = (Function<A, B>) mapper;
        return new MappedStream<>(source, typedMapper.andThen(f));
    }
}

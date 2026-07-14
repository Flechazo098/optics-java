package com.flechazo.hkt.business.stream.internal;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.VTask;
import com.flechazo.hkt.business.stream.VStream;

import java.util.function.BiFunction;

public final class ZipWithStream<A, B, C> implements VStream<C> {
    private final VStream<A> left;
    private final VStream<B> right;
    private final BiFunction<? super A, ? super B, ? extends C> combiner;

    public ZipWithStream(VStream<A> left, VStream<B> right, BiFunction<? super A, ? super B, ? extends C> combiner) {
        this.left = left;
        this.right = right;
        this.combiner = combiner;
    }

    @Override
    public VTask<Step<C>> pull() {
        return left.pull().flatMap(leftStep -> switch (leftStep) {
            case Skip<A> skip -> VTask.pure(new Skip<>(skip.tail().zipWith(right, combiner)));
            case Done<A> ignored -> right.close().map(unused -> new Done<>());
            case Emit<A> leftEmit -> right.pull().flatMap(rightStep -> switch (rightStep) {
                case Skip<B> skip -> VTask.pure(new Skip<>(VStream.concat(VStream.of(leftEmit.value()), leftEmit.tail())
                        .zipWith(skip.tail(), combiner)));
                case Done<B> ignored -> leftEmit.tail().close().map(unused -> new Done<>());
                case Emit<B> rightEmit -> VTask.pure(new Emit<>(
                        combiner.apply(leftEmit.value(), rightEmit.value()),
                        leftEmit.tail().zipWith(rightEmit.tail(), combiner)));
            });
        });
    }

    @Override
    public VTask<Unit> close() {
        return left.close().guarantee(right.close());
    }
}

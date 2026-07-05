package com.flechazo.hkt.business.stream.internal;

import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.stream.VStream;
import com.flechazo.hkt.business.stream.VStream.Done;
import com.flechazo.hkt.business.stream.VStream.Emit;
import com.flechazo.hkt.business.stream.VStream.Skip;
import com.flechazo.hkt.business.stream.VStream.Step;

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
    public Task<Step<C>> pull() {
        return left.pull().flatMap(leftStep -> switch (leftStep) {
            case Skip<A> skip -> Task.pure(new Skip<>(skip.tail().zipWith(right, combiner)));
            case Done<A> ignored -> Task.pure(new Done<>());
            case Emit<A> leftEmit -> right.pull().flatMap(rightStep -> switch (rightStep) {
                case Skip<B> skip -> Task.pure(new Skip<>(VStream.concat(VStream.of(leftEmit.value()), leftEmit.tail())
                        .zipWith(skip.tail(), combiner)));
                case Done<B> ignored -> Task.pure(new Done<>());
                case Emit<B> rightEmit -> Task.pure(new Emit<>(
                        combiner.apply(leftEmit.value(), rightEmit.value()),
                        leftEmit.tail().zipWith(rightEmit.tail(), combiner)));
            });
        });
    }
}

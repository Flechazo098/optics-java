package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.Tuple2;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.business.control.ListPath;
import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.effect.TaskPath;
import com.flechazo.hkt.function.Function3;

import java.util.List;
import java.util.function.*;

public final class VStreamPath<A> implements Chainable<A> {
    private final VStream<A> value;

    public VStreamPath(VStream<A> value) {
        this.value = value;
    }

    public VStream<A> run() {
        return value;
    }

    @Override
    public <B> VStreamPath<B> map(Function<? super A, ? extends B> mapper) {
        return new VStreamPath<>(value.map(mapper));
    }

    @Override
    public VStreamPath<A> peek(Consumer<? super A> consumer) {
        return new VStreamPath<>(value.map(element -> {
            consumer.accept(element);
            return element;
        }));
    }

    @Override
    public <B, C> VStreamPath<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof VStreamPath<?> otherStream)) {
            throw new IllegalArgumentException("Cannot zipWith non-VStreamPath: " + other.getClass());
        }
        VStreamPath<B> typedOther = (VStreamPath<B>) otherStream;
        return new VStreamPath<>(value.zipWith(typedOther.value, combiner));
    }

    @Override
    public <B, C, D> VStreamPath<D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWith(second, Tuple2::new)
                .zipWith(third, (tuple, c) -> combiner.apply(tuple.first(), tuple.second(), c));
    }

    @Override
    public <B> VStreamPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        return new VStreamPath<>(value.flatMap(element -> {
            Chainable<B> mapped = mapper.apply(element);
            if (!(mapped instanceof VStreamPath<?> streamPath)) {
                throw new IllegalArgumentException("via mapper must return VStreamPath, got: " + mapped.getClass());
            }
            return ((VStreamPath<B>) streamPath).value;
        }));
    }

    @Override
    public <B> VStreamPath<B> then(java.util.function.Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    public VStreamPath<A> filter(Predicate<? super A> predicate) {
        return new VStreamPath<>(value.filter(predicate));
    }

    public VStreamPath<A> take(long n) {
        return new VStreamPath<>(value.take(n));
    }

    public VStreamPath<A> drop(long n) {
        return new VStreamPath<>(value.drop(n));
    }

    public VStreamPath<A> takeWhile(Predicate<? super A> predicate) {
        return new VStreamPath<>(value.takeWhile(predicate));
    }

    public VStreamPath<A> dropWhile(Predicate<? super A> predicate) {
        return new VStreamPath<>(value.dropWhile(predicate));
    }

    public VStreamPath<A> distinct() {
        return new VStreamPath<>(value.distinct());
    }

    public VStreamPath<List<A>> chunk(int size) {
        return new VStreamPath<>(value.chunk(size));
    }

    public <B> VStreamPath<B> mapChunked(int size, Function<? super List<A>, ? extends List<B>> mapper) {
        return new VStreamPath<>(value.mapChunked(size, mapper));
    }

    public VStreamPath<A> concat(VStreamPath<A> other) {
        return new VStreamPath<>(value.concat(other.value));
    }

    public VStreamPath<A> prepend(A element) {
        return new VStreamPath<>(value.prepend(element));
    }

    public VStreamPath<A> append(A element) {
        return new VStreamPath<>(value.append(element));
    }

    public VStreamPath<A> interleave(VStreamPath<A> other) {
        return new VStreamPath<>(value.interleave(other.value));
    }

    public TaskPath<List<A>> toList() {
        return new TaskPath<>(value.toList());
    }

    public TaskPath<Maybe<A>> head() {
        return new TaskPath<>(value.head());
    }

    public TaskPath<Maybe<A>> last() {
        return new TaskPath<>(value.last());
    }

    public TaskPath<Long> count() {
        return new TaskPath<>(value.count());
    }

    public TaskPath<Boolean> exists(Predicate<? super A> predicate) {
        return new TaskPath<>(value.exists(predicate));
    }

    public TaskPath<Boolean> forAll(Predicate<? super A> predicate) {
        return new TaskPath<>(value.forAll(predicate));
    }

    public TaskPath<Maybe<A>> find(Predicate<? super A> predicate) {
        return new TaskPath<>(value.find(predicate));
    }

    public TaskPath<Unit> forEach(Consumer<? super A> consumer) {
        return new TaskPath<>(value.forEach(consumer));
    }

    public VStreamPath<A> recover(Function<? super Throwable, ? extends A> recovery) {
        return new VStreamPath<>(value.recover(recovery));
    }

    public VStreamPath<A> recoverWith(Function<? super Throwable, ? extends VStreamPath<A>> recovery) {
        return new VStreamPath<>(value.recoverWith(error -> recovery.apply(error).run()));
    }

    public VStreamPath<A> mapError(Function<? super Throwable, ? extends Throwable> mapper) {
        return new VStreamPath<>(value.mapError(mapper));
    }

    public VStreamPath<A> onError(Consumer<? super Throwable> action) {
        return new VStreamPath<>(value.onError(action));
    }

    public <B> VStreamPath<B> mapTask(Function<? super A, ? extends Task<B>> mapper) {
        return new VStreamPath<>(value.mapTask(mapper));
    }

    public VStreamPath<A> onFinalize(Task<Unit> finalizer) {
        return new VStreamPath<>(value.onFinalize(finalizer));
    }

    public VStreamPath<Unit> asUnit() {
        return new VStreamPath<>(value.asUnit());
    }

    public <M> TaskPath<M> foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper) {
        return new TaskPath<>(value.foldLeft(monoid.empty(), (acc, element) -> monoid.combine(acc, mapper.apply(element))));
    }

    public TaskPath<A> fold(A identity, BinaryOperator<A> op) {
        return new TaskPath<>(value.fold(identity, op));
    }

    public <B> TaskPath<B> foldLeft(B identity, BiFunction<B, A, B> f) {
        return new TaskPath<>(value.foldLeft(identity, f));
    }

    public StreamPath<A> toStreamPath() {
        return new StreamPath<>(value.toStream());
    }

    public ListPath<A> toListPath() {
        return new ListPath<>(value.toList().unsafeRun());
    }
}

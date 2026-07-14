package com.flechazo.hkt.business.stream;


import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.VStreamCombinable;
import com.flechazo.hkt.business.control.ListPath;
import com.flechazo.hkt.business.effect.VTask;
import com.flechazo.hkt.business.effect.VTaskPath;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.*;

public final class VStreamPath<A> implements Chainable<A>, VStreamCombinable<A> {
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
    public <B> VStreamPath<B> then(Supplier<? extends Chainable<B>> supplier) {
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

    public VStreamPath<A> merge(VStreamPath<A> other) {
        return new VStreamPath<>(VStreamPar.merge(value, other.value));
    }

    public Flow.Publisher<A> toPublisher() {
        return value.toPublisher();
    }

    public VStreamPath<A> throttle(int maxElements, Duration window) {
        return new VStreamPath<>(value.throttle(maxElements, window));
    }

    public VStreamPath<A> metered(Duration interval) {
        return new VStreamPath<>(value.metered(interval));
    }

    public VTaskPath<List<A>> toList() {
        return new VTaskPath<>(value.toList());
    }

    public VTaskPath<List<A>> parCollect(int batchSize) {
        return new VTaskPath<>(value.parCollect(batchSize));
    }

    public VTaskPath<Maybe<A>> head() {
        return new VTaskPath<>(value.head());
    }

    public VTaskPath<Maybe<A>> last() {
        return new VTaskPath<>(value.last());
    }

    public VTaskPath<Long> count() {
        return new VTaskPath<>(value.count());
    }

    public VTaskPath<Boolean> exists(Predicate<? super A> predicate) {
        return new VTaskPath<>(value.exists(predicate));
    }

    public VTaskPath<Boolean> forAll(Predicate<? super A> predicate) {
        return new VTaskPath<>(value.forAll(predicate));
    }

    public VTaskPath<Maybe<A>> find(Predicate<? super A> predicate) {
        return new VTaskPath<>(value.find(predicate));
    }

    public VTaskPath<Unit> forEach(Consumer<? super A> consumer) {
        return new VTaskPath<>(value.forEach(consumer));
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

    public <B> VStreamPath<B> mapVTask(Function<? super A, ? extends VTask<B>> mapper) {
        return new VStreamPath<>(value.mapVTask(mapper));
    }

    public <B> VStreamPath<B> parEvalMap(int concurrency, Function<? super A, ? extends VTask<B>> mapper) {
        return new VStreamPath<>(value.parEvalMap(concurrency, mapper));
    }

    public <B> VStreamPath<B> parEvalMapUnordered(int concurrency, Function<? super A, ? extends VTask<B>> mapper) {
        return new VStreamPath<>(value.parEvalMapUnordered(concurrency, mapper));
    }

    public <B> VStreamPath<B> parEvalFlatMap(int concurrency, Function<? super A, ? extends VStreamPath<B>> mapper) {
        return new VStreamPath<>(value.parEvalFlatMap(concurrency, element -> mapper.apply(element).run()));
    }

    public VStreamPath<A> onFinalize(VTask<Unit> finalizer) {
        return new VStreamPath<>(value.onFinalize(finalizer));
    }

    public VStreamPath<Unit> asUnit() {
        return new VStreamPath<>(value.asUnit());
    }

    public <M> VTaskPath<M> foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper) {
        return new VTaskPath<>(value.foldLeft(monoid.empty(), (acc, element) -> monoid.combine(acc, mapper.apply(element))));
    }

    public VTaskPath<A> fold(A identity, BinaryOperator<A> op) {
        return new VTaskPath<>(value.fold(identity, op));
    }

    public <B> VTaskPath<B> foldLeft(B identity, BiFunction<B, A, B> f) {
        return new VTaskPath<>(value.foldLeft(identity, f));
    }

    public StreamPath<A> toStreamPath() {
        return new StreamPath<>(value.toStream());
    }

    public ListPath<A> toListPath() {
        return new ListPath<>(value.toList().unsafeRun());
    }
}

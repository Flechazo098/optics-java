package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.tuple.Tuple3;

import java.util.Objects;
import java.util.function.Function;

public final class IOResourcePath<A> {
    private final IOResource<A> value;

    public IOResourcePath(IOResource<A> value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public IOResource<A> run() {
        return value;
    }

    public IOPath<IOResource.Allocation<A>> allocate() {
        return new IOPath<>(value.allocate());
    }

    public <B> IOResourcePath<B> map(Function<? super A, ? extends B> mapper) {
        return new IOResourcePath<>(value.map(mapper));
    }

    public <B> IOResourcePath<B> flatMap(Function<? super A, IOResourcePath<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new IOResourcePath<>(value.flatMap(resource -> mapper.apply(resource).run()));
    }

    public <B> IOPath<B> use(Function<? super A, IOPath<B>> use) {
        Objects.requireNonNull(use, "use");
        return new IOPath<>(value.use(resource -> use.apply(resource).run()));
    }

    public IOPath<Unit> useVoid(Function<? super A, IOPath<Unit>> use) {
        return use(use);
    }

    public <B> IOPath<B> useSync(Function<? super A, ? extends B> use) {
        return new IOPath<>(value.useSync(use));
    }

    public <B> IOResourcePath<Tuple2<A, B>> and(IOResourcePath<B> other) {
        Objects.requireNonNull(other, "other");
        return new IOResourcePath<>(value.and(other.run()));
    }

    public <B, C> IOResourcePath<Tuple3<A, B, C>> and(IOResourcePath<B> second, IOResourcePath<C> third) {
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        return new IOResourcePath<>(value.and(second.run(), third.run()));
    }

    public IOResourcePath<A> withFinalizer(IOPath<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return new IOResourcePath<>(value.withFinalizer(finalizer.run()));
    }

    public IOResourcePath<A> withFinalizer(IO<Unit> finalizer) {
        return new IOResourcePath<>(value.withFinalizer(finalizer));
    }

    public IOResourcePath<A> withFinalizer(Runnable finalizer) {
        return new IOResourcePath<>(value.withFinalizer(finalizer));
    }

    public IOResourcePath<A> onFailure(Function<? super A, IOPath<Unit>> onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return new IOResourcePath<>(value.onFailure(resource -> onFailure.apply(resource).run()));
    }

    public IOResourcePath<A> onFailureIO(Function<? super A, IO<Unit>> onFailure) {
        return new IOResourcePath<>(value.onFailure(onFailure));
    }

    public IOResourcePath<A> onFailure(Runnable onFailure) {
        return new IOResourcePath<>(value.onFailure(onFailure));
    }

    public ResourcePath<A> toResourcePath() {
        return new ResourcePath<>(value.toResource());
    }
}

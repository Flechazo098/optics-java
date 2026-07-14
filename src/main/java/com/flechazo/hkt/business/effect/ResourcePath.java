package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.tuple.Tuple3;

import java.util.Objects;
import java.util.function.Function;

public final class ResourcePath<A> {
    private final Resource<A> value;

    public ResourcePath(Resource<A> value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public Resource<A> run() {
        return value;
    }

    public VTaskPath<Resource.Allocation<A>> allocate() {
        return new VTaskPath<>(value.allocate());
    }

    public <B> ResourcePath<B> map(Function<? super A, ? extends B> mapper) {
        return new ResourcePath<>(value.map(mapper));
    }

    public <B> ResourcePath<B> flatMap(Function<? super A, ResourcePath<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ResourcePath<>(value.flatMap(resource -> mapper.apply(resource).run()));
    }

    public <B> VTaskPath<B> use(Function<? super A, VTaskPath<B>> use) {
        Objects.requireNonNull(use, "use");
        return new VTaskPath<>(value.use(resource -> use.apply(resource).run()));
    }

    public VTaskPath<Unit> useVoid(Function<? super A, VTaskPath<Unit>> use) {
        return use(use);
    }

    public <B> VTaskPath<B> useSync(Function<? super A, ? extends B> use) {
        return new VTaskPath<>(value.useSync(use));
    }

    public <B> ResourcePath<Tuple2<A, B>> and(ResourcePath<B> other) {
        Objects.requireNonNull(other, "other");
        return new ResourcePath<>(value.and(other.run()));
    }

    public <B, C> ResourcePath<Tuple3<A, B, C>> and(ResourcePath<B> second, ResourcePath<C> third) {
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        return new ResourcePath<>(value.and(second.run(), third.run()));
    }

    public ResourcePath<A> withFinalizer(VTaskPath<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return new ResourcePath<>(value.withFinalizer(finalizer.run()));
    }

    public ResourcePath<A> withFinalizer(VTask<Unit> finalizer) {
        return new ResourcePath<>(value.withFinalizer(finalizer));
    }

    public ResourcePath<A> withFinalizer(Runnable finalizer) {
        return new ResourcePath<>(value.withFinalizer(finalizer));
    }

    public ResourcePath<A> onFailure(Function<? super A, VTaskPath<Unit>> onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return new ResourcePath<>(value.onFailure(resource -> onFailure.apply(resource).run()));
    }

    public ResourcePath<A> onFailureVTask(Function<? super A, VTask<Unit>> onFailure) {
        return new ResourcePath<>(value.onFailure(onFailure));
    }

    public ResourcePath<A> onFailure(Runnable onFailure) {
        return new ResourcePath<>(value.onFailure(onFailure));
    }

    public IOResourcePath<A> toIOResourcePath() {
        return new IOResourcePath<>(value.toIOResource());
    }
}

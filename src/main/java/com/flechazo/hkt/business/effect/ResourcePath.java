package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.Tuple3;

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

    public TaskPath<Resource.Allocation<A>> allocate() {
        return new TaskPath<>(value.allocate());
    }

    public <B> ResourcePath<B> map(Function<? super A, ? extends B> mapper) {
        return new ResourcePath<>(value.map(mapper));
    }

    public <B> ResourcePath<B> flatMap(Function<? super A, ResourcePath<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new ResourcePath<>(value.flatMap(resource -> mapper.apply(resource).run()));
    }

    public <B> TaskPath<B> use(Function<? super A, TaskPath<B>> use) {
        Objects.requireNonNull(use, "use");
        return new TaskPath<>(value.use(resource -> use.apply(resource).run()));
    }

    public TaskPath<Unit> useVoid(Function<? super A, TaskPath<Unit>> use) {
        return use(use);
    }

    public <B> TaskPath<B> useSync(Function<? super A, ? extends B> use) {
        return new TaskPath<>(value.useSync(use));
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

    public ResourcePath<A> withFinalizer(TaskPath<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return new ResourcePath<>(value.withFinalizer(finalizer.run()));
    }

    public ResourcePath<A> withFinalizer(Task<Unit> finalizer) {
        return new ResourcePath<>(value.withFinalizer(finalizer));
    }

    public ResourcePath<A> withFinalizer(Runnable finalizer) {
        return new ResourcePath<>(value.withFinalizer(finalizer));
    }

    public ResourcePath<A> onFailure(Function<? super A, TaskPath<Unit>> onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return new ResourcePath<>(value.onFailure(resource -> onFailure.apply(resource).run()));
    }

    public ResourcePath<A> onFailureTask(Function<? super A, Task<Unit>> onFailure) {
        return new ResourcePath<>(value.onFailure(onFailure));
    }

    public ResourcePath<A> onFailure(Runnable onFailure) {
        return new ResourcePath<>(value.onFailure(onFailure));
    }

    public VIOResourcePath<A> toVIOResourcePath() {
        return new VIOResourcePath<>(value.toVIOResource());
    }
}

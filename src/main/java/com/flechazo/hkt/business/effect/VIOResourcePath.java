package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.Tuple3;

import java.util.Objects;
import java.util.function.Function;

public final class VIOResourcePath<A> {
    private final VIOResource<A> value;

    public VIOResourcePath(VIOResource<A> value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public VIOResource<A> run() {
        return value;
    }

    public VIOPath<VIOResource.Allocation<A>> allocate() {
        return new VIOPath<>(value.allocate());
    }

    public <B> VIOResourcePath<B> map(Function<? super A, ? extends B> mapper) {
        return new VIOResourcePath<>(value.map(mapper));
    }

    public <B> VIOResourcePath<B> flatMap(Function<? super A, VIOResourcePath<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new VIOResourcePath<>(value.flatMap(resource -> mapper.apply(resource).run()));
    }

    public <B> VIOPath<B> use(Function<? super A, VIOPath<B>> use) {
        Objects.requireNonNull(use, "use");
        return new VIOPath<>(value.use(resource -> use.apply(resource).run()));
    }

    public VIOPath<Unit> useVoid(Function<? super A, VIOPath<Unit>> use) {
        return use(use);
    }

    public <B> VIOPath<B> useSync(Function<? super A, ? extends B> use) {
        return new VIOPath<>(value.useSync(use));
    }

    public <B> VIOResourcePath<Tuple2<A, B>> and(VIOResourcePath<B> other) {
        Objects.requireNonNull(other, "other");
        return new VIOResourcePath<>(value.and(other.run()));
    }

    public <B, C> VIOResourcePath<Tuple3<A, B, C>> and(VIOResourcePath<B> second, VIOResourcePath<C> third) {
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        return new VIOResourcePath<>(value.and(second.run(), third.run()));
    }

    public VIOResourcePath<A> withFinalizer(VIOPath<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return new VIOResourcePath<>(value.withFinalizer(finalizer.run()));
    }

    public VIOResourcePath<A> withFinalizer(VIO<Unit> finalizer) {
        return new VIOResourcePath<>(value.withFinalizer(finalizer));
    }

    public VIOResourcePath<A> withFinalizer(Runnable finalizer) {
        return new VIOResourcePath<>(value.withFinalizer(finalizer));
    }

    public VIOResourcePath<A> onFailure(Function<? super A, VIOPath<Unit>> onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return new VIOResourcePath<>(value.onFailure(resource -> onFailure.apply(resource).run()));
    }

    public VIOResourcePath<A> onFailureVIO(Function<? super A, VIO<Unit>> onFailure) {
        return new VIOResourcePath<>(value.onFailure(onFailure));
    }

    public VIOResourcePath<A> onFailure(Runnable onFailure) {
        return new VIOResourcePath<>(value.onFailure(onFailure));
    }

    public ResourcePath<A> toResourcePath() {
        return new ResourcePath<>(value.toResource());
    }
}

package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.tuple.Tuple3;

import java.util.concurrent.Callable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Resource<A> {
    private final VTask<Allocation<A>> allocation;

    public record Allocation<A>(A value, VTask<Unit> release, VTask<Unit> failureCleanup) {
        public Allocation {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(release, "release");
            Objects.requireNonNull(failureCleanup, "failureCleanup");
        }

        public Allocation(A value, VTask<Unit> release) {
            this(value, release, VTask.unit());
        }
    }

    private Resource(VTask<Allocation<A>> allocation) {
        this.allocation = Objects.requireNonNull(allocation, "allocation");
    }

    public static <A> Resource<A> allocate(VTask<Allocation<A>> allocation) {
        return new Resource<>(allocation);
    }

    public static <A> Resource<A> make(Callable<? extends A> acquire, Consumer<? super A> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return of(VTask.of(acquire), value -> VTask.exec(() -> release.accept(value)));
    }

    public static <A> Resource<A> of(VTask<A> acquire, Function<? super A, VTask<Unit>> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return new Resource<>(acquire.map(value ->
                new Allocation<>(value, Objects.requireNonNull(release.apply(value), "release result"))));
    }

    public static <A> Resource<A> of(VTask<A> acquire, VTask<Unit> release) {
        Objects.requireNonNull(release, "release");
        return of(acquire, ignored -> release);
    }

    public static <A extends AutoCloseable> Resource<A> autoCloseable(VTask<A> acquire) {
        return of(acquire, value -> VTask.exec(() -> {
            try {
                value.close();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }));
    }

    public static <A extends AutoCloseable> Resource<A> fromAutoCloseable(Callable<? extends A> acquire) {
        Objects.requireNonNull(acquire, "acquire");
        return autoCloseable(VTask.of(acquire));
    }

    public static <A> Resource<A> pure(A value) {
        return of(VTask.pure(value), ignored -> VTask.unit());
    }

    public VTask<Allocation<A>> allocate() {
        return allocation;
    }

    public <B> Resource<B> map(Function<? super A, ? extends B> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new Resource<>(allocation.map(resource ->
                new Allocation<>(
                        Objects.requireNonNull(mapper.apply(resource.value()), "mapper result"),
                        resource.release(),
                        resource.failureCleanup())));
    }

    public <B> Resource<B> flatMap(Function<? super A, Resource<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new Resource<>(() -> {
            Allocation<A> outer = allocation.execute();
            try {
                Resource<B> innerResource = Objects.requireNonNull(mapper.apply(outer.value()), "mapper result");
                Allocation<B> inner = innerResource.allocate().execute();
                return new Allocation<>(
                        inner.value(),
                        releaseBoth(inner.release(), outer.release()),
                        releaseBoth(inner.failureCleanup(), outer.failureCleanup()));
            } catch (Throwable error) {
                try {
                    outer.release().execute();
                } catch (Throwable releaseError) {
                    error.addSuppressed(releaseError);
                }
                throw error;
            }
        });
    }

    public <B> VTask<B> use(Function<? super A, VTask<B>> use) {
        Objects.requireNonNull(use, "use");
        return () -> {
            Allocation<A> resource = allocation.execute();
            Throwable primary = null;
            try {
                return Objects.requireNonNull(use.apply(resource.value()), "use result").execute();
            } catch (Throwable error) {
                primary = error;
                try {
                    resource.failureCleanup().execute();
                } catch (Throwable cleanupError) {
                    error.addSuppressed(cleanupError);
                }
                throw error;
            } finally {
                try {
                    resource.release().execute();
                } catch (Throwable releaseError) {
                    if (primary != null) {
                        primary.addSuppressed(releaseError);
                    } else {
                        throw releaseError;
                    }
                }
            }
        };
    }

    public <B> VTask<B> useSync(Function<? super A, ? extends B> use) {
        Objects.requireNonNull(use, "use");
        return use(resource -> VTask.delay(() -> Objects.requireNonNull(use.apply(resource), "use result")));
    }

    public VTask<Unit> useVoid(Function<? super A, VTask<Unit>> use) {
        return use(use);
    }

    public <B> Resource<Tuple2<A, B>> and(Resource<B> other) {
        Objects.requireNonNull(other, "other");
        return flatMap(left -> other.map(right -> Tuple2.of(left, right)));
    }

    public <B, C> Resource<Tuple3<A, B, C>> and(Resource<B> second, Resource<C> third) {
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        return and(second).flatMap(pair -> third.map(value -> Tuple3.of(pair.first(), pair.second(), value)));
    }

    public Resource<A> withFinalizer(VTask<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return new Resource<>(allocation.map(resource ->
                new Allocation<>(resource.value(), releaseBoth(resource.release(), finalizer), resource.failureCleanup())));
    }

    public Resource<A> withFinalizer(Runnable finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return withFinalizer(VTask.exec(finalizer));
    }

    public Resource<A> onFailure(Function<? super A, VTask<Unit>> onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return new Resource<>(allocation.map(resource ->
                new Allocation<>(
                        resource.value(),
                        resource.release(),
                        releaseBoth(
                                resource.failureCleanup(),
                                Objects.requireNonNull(onFailure.apply(resource.value()), "onFailure result")))));
    }

    public Resource<A> onFailure(Runnable onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return onFailure(ignored -> VTask.exec(onFailure));
    }

    public IOResource<A> toIOResource() {
        return IOResource.allocate(allocation.map(resource ->
                new IOResource.Allocation<>(
                        resource.value(),
                        resource.release().toIO(),
                        resource.failureCleanup().toIO())).toIO());
    }

    private static VTask<Unit> releaseBoth(VTask<Unit> first, VTask<Unit> second) {
        return () -> {
            Throwable firstError = null;
            try {
                first.execute();
            } catch (Throwable error) {
                firstError = error;
            }
            try {
                second.execute();
            } catch (Throwable secondError) {
                if (firstError != null) {
                    firstError.addSuppressed(secondError);
                    throw firstError;
                }
                throw secondError;
            }
            if (firstError != null) {
                throw firstError;
            }
            return Unit.INSTANCE;
        };
    }
}

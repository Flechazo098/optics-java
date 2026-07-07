package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.Tuple3;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public final class VIOResource<A> {
    private final VIO<Allocation<A>> allocation;

    public record Allocation<A>(A value, VIO<Unit> release, VIO<Unit> failureCleanup) {
        public Allocation {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(release, "release");
            Objects.requireNonNull(failureCleanup, "failureCleanup");
        }

        public Allocation(A value, VIO<Unit> release) {
            this(value, release, VIO.unit());
        }
    }

    private VIOResource(VIO<Allocation<A>> allocation) {
        this.allocation = Objects.requireNonNull(allocation, "allocation");
    }

    public static <A> VIOResource<A> allocate(VIO<Allocation<A>> allocation) {
        return new VIOResource<>(allocation);
    }

    public static <A> VIOResource<A> make(Callable<? extends A> acquire, Consumer<? super A> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return of(VIO.delay(acquire::call), value -> VIO.exec(() -> release.accept(value)));
    }

    public static <A> VIOResource<A> of(VIO<A> acquire, Function<? super A, VIO<Unit>> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return new VIOResource<>(acquire.map(value ->
                new Allocation<>(value, Objects.requireNonNull(release.apply(value), "release result"))));
    }

    public static <A> VIOResource<A> of(VIO<A> acquire, VIO<Unit> release) {
        Objects.requireNonNull(release, "release");
        return of(acquire, ignored -> release);
    }

    public static <A extends AutoCloseable> VIOResource<A> autoCloseable(VIO<A> acquire) {
        return of(acquire, value -> VIO.exec(() -> {
            try {
                value.close();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }));
    }

    public static <A extends AutoCloseable> VIOResource<A> fromAutoCloseable(Callable<? extends A> acquire) {
        Objects.requireNonNull(acquire, "acquire");
        return autoCloseable(VIO.delay(acquire::call));
    }

    public static <A> VIOResource<A> pure(A value) {
        return of(VIO.pure(value), ignored -> VIO.unit());
    }

    public VIO<Allocation<A>> allocate() {
        return allocation;
    }

    public <B> VIOResource<B> map(Function<? super A, ? extends B> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new VIOResource<>(allocation.map(resource ->
                new Allocation<>(
                        Objects.requireNonNull(mapper.apply(resource.value()), "mapper result"),
                        resource.release(),
                        resource.failureCleanup())));
    }

    public <B> VIOResource<B> flatMap(Function<? super A, VIOResource<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new VIOResource<>(() -> {
            Allocation<A> outer = allocation.unsafeRun();
            try {
                VIOResource<B> innerResource = Objects.requireNonNull(mapper.apply(outer.value()), "mapper result");
                Allocation<B> inner = innerResource.allocate().unsafeRun();
                return new Allocation<>(
                        inner.value(),
                        releaseBoth(inner.release(), outer.release()),
                        releaseBoth(inner.failureCleanup(), outer.failureCleanup()));
            } catch (Throwable error) {
                try {
                    outer.release().unsafeRun();
                } catch (Throwable releaseError) {
                    error.addSuppressed(releaseError);
                }
                throw toException(error);
            }
        });
    }

    public <B> VIO<B> use(Function<? super A, VIO<B>> use) {
        Objects.requireNonNull(use, "use");
        return () -> {
            Allocation<A> resource = allocation.unsafeRun();
            Throwable primary = null;
            try {
                return Objects.requireNonNull(use.apply(resource.value()), "use result").unsafeRun();
            } catch (Throwable error) {
                primary = error;
                try {
                    resource.failureCleanup().unsafeRun();
                } catch (Throwable cleanupError) {
                    error.addSuppressed(cleanupError);
                }
                throw toException(error);
            } finally {
                try {
                    resource.release().unsafeRun();
                } catch (Throwable releaseError) {
                    if (primary != null) {
                        primary.addSuppressed(releaseError);
                    } else {
                        throw toException(releaseError);
                    }
                }
            }
        };
    }

    public VIO<Unit> useVoid(Function<? super A, VIO<Unit>> use) {
        return use(use);
    }

    public <B> VIO<B> useSync(Function<? super A, ? extends B> use) {
        Objects.requireNonNull(use, "use");
        return use(resource -> VIO.delay(() -> Objects.requireNonNull(use.apply(resource), "use result")));
    }

    public <B> VIOResource<Tuple2<A, B>> and(VIOResource<B> other) {
        Objects.requireNonNull(other, "other");
        return flatMap(left -> other.map(right -> Tuple2.of(left, right)));
    }

    public <B, C> VIOResource<Tuple3<A, B, C>> and(VIOResource<B> second, VIOResource<C> third) {
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        return and(second).flatMap(pair -> third.map(value -> Tuple3.of(pair.first(), pair.second(), value)));
    }

    public VIOResource<A> withFinalizer(VIO<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return new VIOResource<>(allocation.map(resource ->
                new Allocation<>(resource.value(), releaseBoth(resource.release(), finalizer), resource.failureCleanup())));
    }

    public VIOResource<A> withFinalizer(Runnable finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return withFinalizer(VIO.exec(finalizer));
    }

    public VIOResource<A> onFailure(Function<? super A, VIO<Unit>> onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return new VIOResource<>(allocation.map(resource ->
                new Allocation<>(
                        resource.value(),
                        resource.release(),
                        releaseBoth(
                                resource.failureCleanup(),
                                Objects.requireNonNull(onFailure.apply(resource.value()), "onFailure result")))));
    }

    public VIOResource<A> onFailure(Runnable onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return onFailure(ignored -> VIO.exec(onFailure));
    }

    public Resource<A> toResource() {
        return Resource.allocate(allocation.map(resource ->
                new Resource.Allocation<>(
                        resource.value(),
                        resource.release().toTask(),
                        resource.failureCleanup().toTask())).toTask());
    }

    private static VIO<Unit> releaseBoth(VIO<Unit> first, VIO<Unit> second) {
        return () -> {
            Throwable firstError = null;
            try {
                first.unsafeRun();
            } catch (Throwable error) {
                firstError = error;
            }
            try {
                second.unsafeRun();
            } catch (Throwable secondError) {
                if (firstError != null) {
                    firstError.addSuppressed(secondError);
                    throw toException(firstError);
                }
                throw toException(secondError);
            }
            if (firstError != null) {
                throw toException(firstError);
            }
            return Unit.INSTANCE;
        };
    }

    private static Exception toException(Throwable error) {
        if (error instanceof Exception exception) {
            return exception;
        }
        if (error instanceof Error fatal) {
            throw fatal;
        }
        return new RuntimeException(error);
    }
}

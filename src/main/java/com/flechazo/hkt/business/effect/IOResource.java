package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.tuple.Tuple3;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public final class IOResource<A> {
    private final IO<Allocation<A>> allocation;

    public record Allocation<A>(A value, IO<Unit> release, IO<Unit> failureCleanup) {
        public Allocation {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(release, "release");
            Objects.requireNonNull(failureCleanup, "failureCleanup");
        }

        public Allocation(A value, IO<Unit> release) {
            this(value, release, IO.unit());
        }
    }

    private IOResource(IO<Allocation<A>> allocation) {
        this.allocation = Objects.requireNonNull(allocation, "allocation");
    }

    public static <A> IOResource<A> allocate(IO<Allocation<A>> allocation) {
        return new IOResource<>(allocation);
    }

    public static <A> IOResource<A> make(Callable<? extends A> acquire, Consumer<? super A> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return of(IO.delay(acquire::call), value -> IO.exec(() -> release.accept(value)));
    }

    public static <A> IOResource<A> of(IO<A> acquire, Function<? super A, IO<Unit>> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return new IOResource<>(acquire.map(value ->
                new Allocation<>(value, Objects.requireNonNull(release.apply(value), "release result"))));
    }

    public static <A> IOResource<A> of(IO<A> acquire, IO<Unit> release) {
        Objects.requireNonNull(release, "release");
        return of(acquire, ignored -> release);
    }

    public static <A extends AutoCloseable> IOResource<A> autoCloseable(IO<A> acquire) {
        return of(acquire, value -> IO.exec(() -> {
            try {
                value.close();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }));
    }

    public static <A extends AutoCloseable> IOResource<A> fromAutoCloseable(Callable<? extends A> acquire) {
        Objects.requireNonNull(acquire, "acquire");
        return autoCloseable(IO.delay(acquire::call));
    }

    public static <A> IOResource<A> pure(A value) {
        return of(IO.pure(value), ignored -> IO.unit());
    }

    public IO<Allocation<A>> allocate() {
        return allocation;
    }

    public <B> IOResource<B> map(Function<? super A, ? extends B> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new IOResource<>(allocation.map(resource ->
                new Allocation<>(
                        Objects.requireNonNull(mapper.apply(resource.value()), "mapper result"),
                        resource.release(),
                        resource.failureCleanup())));
    }

    public <B> IOResource<B> flatMap(Function<? super A, IOResource<B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new IOResource<>(() -> {
            Allocation<A> outer = allocation.unsafeRun();
            try {
                IOResource<B> innerResource = Objects.requireNonNull(mapper.apply(outer.value()), "mapper result");
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

    public <B> IO<B> use(Function<? super A, IO<B>> use) {
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

    public IO<Unit> useVoid(Function<? super A, IO<Unit>> use) {
        return use(use);
    }

    public <B> IO<B> useSync(Function<? super A, ? extends B> use) {
        Objects.requireNonNull(use, "use");
        return use(resource -> IO.delay(() -> Objects.requireNonNull(use.apply(resource), "use result")));
    }

    public <B> IOResource<Tuple2<A, B>> and(IOResource<B> other) {
        Objects.requireNonNull(other, "other");
        return flatMap(left -> other.map(right -> Tuple2.of(left, right)));
    }

    public <B, C> IOResource<Tuple3<A, B, C>> and(IOResource<B> second, IOResource<C> third) {
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        return and(second).flatMap(pair -> third.map(value -> Tuple3.of(pair.first(), pair.second(), value)));
    }

    public IOResource<A> withFinalizer(IO<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return new IOResource<>(allocation.map(resource ->
                new Allocation<>(resource.value(), releaseBoth(resource.release(), finalizer), resource.failureCleanup())));
    }

    public IOResource<A> withFinalizer(Runnable finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return withFinalizer(IO.exec(finalizer));
    }

    public IOResource<A> onFailure(Function<? super A, IO<Unit>> onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return new IOResource<>(allocation.map(resource ->
                new Allocation<>(
                        resource.value(),
                        resource.release(),
                        releaseBoth(
                                resource.failureCleanup(),
                                Objects.requireNonNull(onFailure.apply(resource.value()), "onFailure result")))));
    }

    public IOResource<A> onFailure(Runnable onFailure) {
        Objects.requireNonNull(onFailure, "onFailure");
        return onFailure(ignored -> IO.exec(onFailure));
    }

    public Resource<A> toResource() {
        return Resource.allocate(allocation.map(resource ->
                new Resource.Allocation<>(
                        resource.value(),
                        resource.release().toVTask(),
                        resource.failureCleanup().toVTask())).toVTask());
    }

    private static IO<Unit> releaseBoth(IO<Unit> first, IO<Unit> second) {
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

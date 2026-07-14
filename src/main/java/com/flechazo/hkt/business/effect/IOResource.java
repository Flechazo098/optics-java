package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.tuple.Tuple3;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents acquisition, use, and release of a managed synchronous resource.
 *
 * @param <A> the resource type
 */
public final class IOResource<A> {
    private final IO<Allocation<A>> allocation;

    /**
     * Contains an acquired value and its release actions.
     *
     * @param <A> the resource type
     * @param value the acquired resource
     * @param release the action run after use
     * @param failureCleanup the additional action run when use fails
     */
    public record Allocation<A>(A value, IO<Unit> release, IO<Unit> failureCleanup) {
        /**
         * Creates an allocation.
         *
         * @param value the acquired resource
         * @param release the action run after use
         * @param failureCleanup the additional action run when use fails
         */
        public Allocation {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(release, "release");
            Objects.requireNonNull(failureCleanup, "failureCleanup");
        }

        /**
         * Creates an allocation without an additional failure-cleanup action.
         *
         * @param value the acquired resource
         * @param release the action run after use
         */
        public Allocation(A value, IO<Unit> release) {
            this(value, release, IO.unit());
        }
    }

    private IOResource(IO<Allocation<A>> allocation) {
        this.allocation = Objects.requireNonNull(allocation, "allocation");
    }

    /**
     * Creates a resource from an allocation computation.
     *
     * @param <A> the resource type
     * @param allocation the deferred allocation
     * @return the managed resource
     */
    public static <A> IOResource<A> allocate(IO<Allocation<A>> allocation) {
        return new IOResource<>(allocation);
    }

    /**
     * Creates a resource from blocking acquisition and release operations.
     *
     * @param <A> the resource type
     * @param acquire the acquisition operation
     * @param release the release operation
     * @return the managed resource
     */
    public static <A> IOResource<A> make(Callable<? extends A> acquire, Consumer<? super A> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return of(IO.delay(acquire::call), value -> IO.exec(() -> release.accept(value)));
    }

    /**
     * Creates a resource from acquisition and value-dependent release computations.
     *
     * @param <A> the resource type
     * @param acquire the acquisition computation
     * @param release the release computation selected from the acquired value
     * @return the managed resource
     */
    public static <A> IOResource<A> of(IO<A> acquire, Function<? super A, IO<Unit>> release) {
        Objects.requireNonNull(acquire, "acquire");
        Objects.requireNonNull(release, "release");
        return new IOResource<>(acquire.map(value ->
                new Allocation<>(value, Objects.requireNonNull(release.apply(value), "release result"))));
    }

    /**
     * Creates a resource from acquisition and common release computations.
     *
     * @param <A> the resource type
     * @param acquire the acquisition computation
     * @param release the release computation
     * @return the managed resource
     */
    public static <A> IOResource<A> of(IO<A> acquire, IO<Unit> release) {
        Objects.requireNonNull(release, "release");
        return of(acquire, ignored -> release);
    }

    /**
     * Creates a resource that closes the acquired value after use.
     *
     * @param <A> the closeable resource type
     * @param acquire the acquisition computation
     * @return the managed closeable resource
     */
    public static <A extends AutoCloseable> IOResource<A> autoCloseable(IO<A> acquire) {
        return of(acquire, value -> IO.exec(() -> {
            try {
                value.close();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }));
    }

    /**
     * Creates a resource that acquires and closes a blocking value.
     *
     * @param <A> the closeable resource type
     * @param acquire the acquisition operation
     * @return the managed closeable resource
     */
    public static <A extends AutoCloseable> IOResource<A> fromAutoCloseable(Callable<? extends A> acquire) {
        Objects.requireNonNull(acquire, "acquire");
        return autoCloseable(IO.delay(acquire::call));
    }

    /**
     * Creates a resource containing a value with no release action.
     *
     * @param <A> the resource type
     * @param value the resource value
     * @return the pure managed resource
     */
    public static <A> IOResource<A> pure(A value) {
        return of(IO.pure(value), ignored -> IO.unit());
    }

    /**
     * Returns the deferred allocation.
     *
     * @return the allocation computation
     */
    public IO<Allocation<A>> allocate() {
        return allocation;
    }

    /**
     * Transforms the acquired value while preserving release actions.
     *
     * @param <B> the transformed resource type
     * @param mapper the resource transformation
     * @return the transformed managed resource
     */
    public <B> IOResource<B> map(Function<? super A, ? extends B> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new IOResource<>(allocation.map(resource ->
                new Allocation<>(
                        Objects.requireNonNull(mapper.apply(resource.value()), "mapper result"),
                        resource.release(),
                        resource.failureCleanup())));
    }

    /**
     * Sequences a managed resource selected from the acquired value.
     *
     * @param <B> the next resource type
     * @param mapper the function selecting the next resource
     * @return a resource that releases both acquisitions in reverse order
     */
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

    /**
     * Uses the acquired resource and releases it after completion.
     *
     * @param <B> the use result type
     * @param use the computation selected from the acquired resource
     * @return a computation producing the use result
     */
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

    /**
     * Uses the acquired resource for an action returning {@link Unit}.
     *
     * @param use the action selected from the acquired resource
     * @return the managed use computation
     */
    public IO<Unit> useVoid(Function<? super A, IO<Unit>> use) {
        return use(use);
    }

    /**
     * Uses the acquired resource with a synchronous function.
     *
     * @param <B> the use result type
     * @param use the resource function
     * @return the managed use computation
     */
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

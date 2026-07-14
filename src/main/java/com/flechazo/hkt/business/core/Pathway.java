package com.flechazo.hkt.business.core;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.data.NonEmptyList;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.StreamK;
import com.flechazo.hkt.business.stream.StreamPath;
import com.flechazo.hkt.business.stream.VStream;
import com.flechazo.hkt.business.stream.VStreamPath;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Provides entry points for constructing fluent business computation paths.
 */
public final class Pathway {
    private Pathway() {
    }

    /**
     * Creates a maybe path containing a value.
     *
     * @param <A> the value type
     * @param value the contained value
     * @return a defined maybe path
     */
    public static <A> MaybePath<A> just(A value) {
        return new MaybePath<>(Maybe.some(value));
    }

    /**
     * Creates an empty maybe path.
     *
     * @param <A> the value type
     * @return an empty maybe path
     */
    public static <A> MaybePath<A> nothing() {
        return new MaybePath<>(Maybe.none());
    }

    /**
     * Creates a maybe path from a nullable value.
     *
     * @param <A> the value type
     * @param value the possibly null value
     * @return a defined path for a non-null value, otherwise an empty path
     */
    public static <A> MaybePath<A> nullable(@Nullable A value) {
        return new MaybePath<>(Maybe.ofNullable(value));
    }

    /**
     * Wraps a maybe value in a fluent path.
     *
     * @param <A> the value type
     * @param value the maybe value
     * @return a path over {@code value}
     */
    public static <A> MaybePath<A> maybe(Maybe<A> value) {
        return new MaybePath<>(value);
    }

    /**
     * Creates an either path containing a right value.
     *
     * @param <E> the error type
     * @param <A> the result type
     * @param value the right value
     * @return a right either path
     */
    public static <E, A> EitherPath<E, A> right(A value) {
        return new EitherPath<>(Either.right(value));
    }

    /**
     * Creates an either path containing a left error.
     *
     * @param <E> the error type
     * @param <A> the result type
     * @param error the left error
     * @return a left either path
     */
    public static <E, A> EitherPath<E, A> left(E error) {
        return new EitherPath<>(Either.left(error));
    }

    /**
     * Wraps an either value in a fluent path.
     *
     * @param <E> the error type
     * @param <A> the result type
     * @param value the either value
     * @return a path over {@code value}
     */
    public static <E, A> EitherPath<E, A> either(Either<E, A> value) {
        return new EitherPath<>(value);
    }

    /**
     * Creates a virtual-thread task path from a checked supplier.
     *
     * @param <A> the result type
     * @param supplier the deferred computation
     * @return a deferred task path
     */
    public static <A> VTaskPath<A> vtask(CheckedSupplier<? extends A, ?> supplier) {
        return new VTaskPath<>(VTask.delay(supplier));
    }

    /**
     * Creates a try path by executing a checked supplier.
     *
     * @param <A> the result type
     * @param supplier the computation to execute
     * @return a path containing the successful result or captured failure
     */
    public static <A> TryPath<A> tryOf(CheckedSupplier<? extends A, ?> supplier) {
        return new TryPath<>(Try.of(supplier));
    }

    /**
     * Creates a successful try path.
     *
     * @param <A> the result type
     * @param value the successful value
     * @return a successful try path
     */
    public static <A> TryPath<A> success(A value) {
        return new TryPath<>(Try.success(value));
    }

    /**
     * Creates a failed try path.
     *
     * @param <A> the result type
     * @param error the failure cause
     * @return a failed try path
     */
    public static <A> TryPath<A> failure(Throwable error) {
        return new TryPath<>(Try.failure(error));
    }

    /**
     * Wraps a try value in a fluent path.
     *
     * @param <A> the result type
     * @param value the try value
     * @return a path over {@code value}
     */
    public static <A> TryPath<A> tryPath(Try<A> value) {
        return new TryPath<>(value);
    }

    /**
     * Creates an IO path from a checked supplier.
     *
     * @param <A> the result type
     * @param supplier the deferred computation
     * @return a deferred IO path
     */
    public static <A> IOPath<A> io(CheckedSupplier<? extends A, ?> supplier) {
        return new IOPath<>(IO.delay(supplier));
    }

    /**
     * Creates an IO path from a runnable action.
     *
     * @param runnable the deferred action
     * @return an IO path producing {@link Unit}
     */
    public static IOPath<Unit> ioRunnable(Runnable runnable) {
        return new IOPath<>(IO.exec(runnable));
    }

    /**
     * Creates an IO path containing a pure value.
     *
     * @param <A> the result type
     * @param value the result value
     * @return a successful IO path
     */
    public static <A> IOPath<A> ioPure(A value) {
        return new IOPath<>(IO.pure(value));
    }

    /**
     * Creates an IO path that fails with a cause.
     *
     * @param <A> the result type
     * @param error the failure cause
     * @return a failed IO path
     */
    public static <A> IOPath<A> ioFail(Throwable error) {
        return new IOPath<>(IO.failed(error));
    }

    /**
     * Wraps an IO computation in a fluent path.
     *
     * @param <A> the result type
     * @param value the IO computation
     * @return a path over {@code value}
     */
    public static <A> IOPath<A> ioPath(IO<A> value) {
        return new IOPath<>(value);
    }

    /**
     * Creates a resource path from acquisition and release tasks.
     *
     * @param <A> the resource type
     * @param acquire the acquisition task
     * @param release the release task selected from the acquired resource
     * @return a managed resource path
     */
    public static <A> ResourcePath<A> resource(VTask<A> acquire, Function<? super A, VTask<Unit>> release) {
        return new ResourcePath<>(Resource.of(acquire, release));
    }

    /**
     * Creates a resource path from blocking acquisition and release operations.
     *
     * @param <A> the resource type
     * @param acquire the acquisition operation
     * @param release the release operation
     * @return a managed resource path
     */
    public static <A> ResourcePath<A> resourceMake(Callable<? extends A> acquire, Consumer<? super A> release) {
        return new ResourcePath<>(Resource.make(acquire, release));
    }

    /**
     * Creates a resource path that closes an acquired resource.
     *
     * @param <A> the closeable resource type
     * @param acquire the acquisition task
     * @return a managed resource path
     */
    public static <A extends AutoCloseable> ResourcePath<A> autoResource(VTask<A> acquire) {
        return new ResourcePath<>(Resource.autoCloseable(acquire));
    }

    /**
     * Creates a resource path that acquires and closes a blocking resource.
     *
     * @param <A> the closeable resource type
     * @param acquire the acquisition operation
     * @return a managed resource path
     */
    public static <A extends AutoCloseable> ResourcePath<A> autoResource(Callable<? extends A> acquire) {
        return new ResourcePath<>(Resource.fromAutoCloseable(acquire));
    }

    /**
     * Wraps a managed virtual-thread resource in a fluent path.
     *
     * @param <A> the resource type
     * @param value the managed resource
     * @return a path over {@code value}
     */
    public static <A> ResourcePath<A> resourcePath(Resource<A> value) {
        return new ResourcePath<>(value);
    }

    /**
     * Creates an IO resource path from acquisition and release computations.
     *
     * @param <A> the resource type
     * @param acquire the acquisition computation
     * @param release the release computation selected from the acquired resource
     * @return a managed IO resource path
     */
    public static <A> IOResourcePath<A> ioResource(IO<A> acquire, Function<? super A, IO<Unit>> release) {
        return new IOResourcePath<>(IOResource.of(acquire, release));
    }

    /**
     * Creates an IO resource path from blocking acquisition and release operations.
     *
     * @param <A> the resource type
     * @param acquire the acquisition operation
     * @param release the release operation
     * @return a managed IO resource path
     */
    public static <A> IOResourcePath<A> ioResourceMake(Callable<? extends A> acquire, Consumer<? super A> release) {
        return new IOResourcePath<>(IOResource.make(acquire, release));
    }

    /**
     * Creates an IO resource path that closes an acquired resource.
     *
     * @param <A> the closeable resource type
     * @param acquire the acquisition computation
     * @return a managed IO resource path
     */
    public static <A extends AutoCloseable> IOResourcePath<A> ioAutoResource(IO<A> acquire) {
        return new IOResourcePath<>(IOResource.autoCloseable(acquire));
    }

    /**
     * Creates an IO resource path that acquires and closes a blocking resource.
     *
     * @param <A> the closeable resource type
     * @param acquire the acquisition operation
     * @return a managed IO resource path
     */
    public static <A extends AutoCloseable> IOResourcePath<A> ioAutoResource(Callable<? extends A> acquire) {
        return new IOResourcePath<>(IOResource.fromAutoCloseable(acquire));
    }

    /**
     * Wraps a managed IO resource in a fluent path.
     *
     * @param <A> the resource type
     * @param value the managed resource
     * @return a path over {@code value}
     */
    public static <A> IOResourcePath<A> ioResourcePath(IOResource<A> value) {
        return new IOResourcePath<>(value);
    }

    /**
     * Creates a virtual-thread task path from a runnable action.
     *
     * @param runnable the deferred action
     * @return a task path producing {@link Unit}
     */
    public static VTaskPath<Unit> vtask(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return new VTaskPath<>(VTask.delay(() -> {
            runnable.run();
            return Unit.INSTANCE;
        }));
    }

    /**
     * Wraps a virtual-thread task in a fluent path.
     *
     * @param <A> the result type
     * @param value the task
     * @return a path over {@code value}
     */
    public static <A> VTaskPath<A> vtask(VTask<A> value) {
        return new VTaskPath<>(value);
    }

    /**
     * Wraps a virtual-thread task in a fluent path.
     *
     * @param <A> the result type
     * @param value the task
     * @return a path over {@code value}
     */
    public static <A> VTaskPath<A> vtaskPath(VTask<A> value) {
        return new VTaskPath<>(value);
    }

    /**
     * Creates a task path from a deferred completion stage.
     *
     * @param <A> the result type
     * @param supplier the supplier of the future
     * @return a task path awaiting the supplied future
     */
    public static <A> VTaskPath<A> future(Supplier<? extends CompletableFuture<A>> supplier) {
        return new VTaskPath<>(VTask.async(supplier));
    }

    /**
     * Creates a successful virtual-thread task path.
     *
     * @param <A> the result type
     * @param value the result value
     * @return a successful task path
     */
    public static <A> VTaskPath<A> vtaskPure(A value) {
        return new VTaskPath<>(VTask.pure(value));
    }

    /**
     * Creates a virtual-thread task path that fails with a cause.
     *
     * @param <A> the result type
     * @param error the failure cause
     * @return a failed task path
     */
    public static <A> VTaskPath<A> vtaskFail(Throwable error) {
        return new VTaskPath<>(VTask.failed(error));
    }

    /**
     * Creates a valid value.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param value the valid value
     * @return a valid result
     */
    public static <E, A> Validated<E, A> valid(A value) {
        return Validated.valid(value);
    }

    /**
     * Creates an invalid value.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param error the validation error
     * @return an invalid result
     */
    public static <E, A> Validated<E, A> invalid(E error) {
        return Validated.invalid(error);
    }

    /**
     * Creates a valid validation path with an error semigroup.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param value the valid value
     * @param semigroup the operation used to accumulate errors
     * @return a valid validation path
     */
    public static <E, A> ValidationPath<E, A> valid(A value, Semigroup<E> semigroup) {
        return new ValidationPath<>(Validated.valid(value), semigroup);
    }

    /**
     * Creates an invalid validation path with an error semigroup.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param error the validation error
     * @param semigroup the operation used to accumulate errors
     * @return an invalid validation path
     */
    public static <E, A> ValidationPath<E, A> invalid(E error, Semigroup<E> semigroup) {
        return new ValidationPath<>(Validated.invalid(error), semigroup);
    }

    /**
     * Creates a valid path that accumulates errors in a non-empty list.
     *
     * @param <E> the validation error element type
     * @param <A> the valid value type
     * @param value the valid value
     * @return a valid accumulating path
     */
    public static <E, A> ValidationPath<NonEmptyList<E>, A> validNel(A value) {
        return new ValidationPath<>(ValidatedNel.valid(value), NonEmptyList.semigroup());
    }

    /**
     * Creates an invalid path containing one error in a non-empty list.
     *
     * @param <E> the validation error element type
     * @param <A> the valid value type
     * @param error the validation error
     * @return an invalid accumulating path
     */
    public static <E, A> ValidationPath<NonEmptyList<E>, A> invalidNel(E error) {
        return new ValidationPath<>(ValidatedNel.invalid(error), NonEmptyList.semigroup());
    }

    /**
     * Returns the concatenation semigroup for non-empty error lists.
     *
     * @param <E> the error element type
     * @return the non-empty-list semigroup
     */
    public static <E> Semigroup<NonEmptyList<E>> nelSemigroup() {
        return NonEmptyList.semigroup();
    }

    /**
     * Creates a list path from encountered elements.
     *
     * @param <A> the element type
     * @param values the source elements
     * @return a path containing the encountered elements
     */
    public static <A> ListPath<A> listPath(List<? extends A> values) {
        return new ListPath<>(values);
    }

    /**
     * Creates a list path from arguments in declaration order.
     *
     * @param <A> the element type
     * @param values the path elements
     * @return a path containing {@code values}
     */
    @SafeVarargs
    public static <A> ListPath<A> listPath(A... values) {
        return new ListPath<>(List.of(values));
    }

    /**
     * Creates a singleton list path.
     *
     * @param <A> the element type
     * @param value the sole element
     * @return a singleton list path
     */
    public static <A> ListPath<A> listPathPure(A value) {
        return new ListPath<>(List.of(value));
    }

    /**
     * Creates an empty list path.
     *
     * @param <A> the element type
     * @return an empty list path
     */
    public static <A> ListPath<A> listPathEmpty() {
        return new ListPath<>(List.of());
    }

    /**
     * Creates an integer list path over a half-open range.
     *
     * @param startInclusive the first value
     * @param endExclusive the exclusive upper bound
     * @return a list path containing the range values
     */
    public static ListPath<Integer> listPathRange(int startInclusive, int endExclusive) {
        ArrayList<Integer> values = new ArrayList<>(Math.max(endExclusive - startInclusive, 0));
        for (int i = startInclusive; i < endExclusive; i++) {
            values.add(i);
        }
        return new ListPath<>(values);
    }

    /**
     * Wraps a reader computation in a fluent path.
     *
     * @param <R> the environment type
     * @param <A> the result type
     * @param reader the reader computation
     * @return a path over {@code reader}
     */
    public static <R, A> ReaderPath<R, A> reader(Reader<R, A> reader) {
        return new ReaderPath<>(reader);
    }

    /**
     * Creates a reader path that ignores its environment and returns a value.
     *
     * @param <R> the environment type
     * @param <A> the result type
     * @param value the result value
     * @return a constant reader path
     */
    public static <R, A> ReaderPath<R, A> readerPure(A value) {
        return ReaderPath.pure(value);
    }

    /**
     * Creates a reader path that returns its environment.
     *
     * @param <R> the environment type
     * @return an environment-reading path
     */
    public static <R> ReaderPath<R, R> ask() {
        return ReaderPath.ask();
    }

    /**
     * Creates a reader path that derives a result from its environment.
     *
     * @param <R> the environment type
     * @param <A> the result type
     * @param mapper the environment projection
     * @return a path applying {@code mapper} to its environment
     */
    public static <R, A> ReaderPath<R, A> asks(Function<? super R, ? extends A> mapper) {
        return ReaderPath.asks(mapper);
    }

    /**
     * Wraps a stateful computation in a fluent path.
     *
     * @param <S> the state type
     * @param <A> the result type
     * @param state the stateful computation
     * @return a path over {@code state}
     */
    public static <S, A> WithStatePath<S, A> state(State<S, A> state) {
        return new WithStatePath<>(state);
    }

    /**
     * Creates a state path that preserves state and returns a value.
     *
     * @param <S> the state type
     * @param <A> the result type
     * @param value the result value
     * @return a pure state path
     */
    public static <S, A> WithStatePath<S, A> statePure(A value) {
        return WithStatePath.pure(value);
    }

    /**
     * Creates a state path that returns the current state.
     *
     * @param <S> the state type
     * @return a state-reading path
     */
    public static <S> WithStatePath<S, S> getState() {
        return WithStatePath.get();
    }

    /**
     * Creates a state path that replaces the current state.
     *
     * @param <S> the state type
     * @param newState the replacement state
     * @return a state path producing {@link Unit}
     */
    public static <S> WithStatePath<S, Unit> setState(S newState) {
        return WithStatePath.set(newState);
    }

    /**
     * Creates a state path that transforms the current state.
     *
     * @param <S> the state type
     * @param mapper the state transformation
     * @return a state path producing {@link Unit}
     */
    public static <S> WithStatePath<S, Unit> modifyState(UnaryOperator<S> mapper) {
        return WithStatePath.modify(mapper);
    }

    /**
     * Wraps a writer computation in a fluent path.
     *
     * @param <W> the output type
     * @param <A> the result type
     * @param writer the writer computation
     * @param monoid the operation used to combine output
     * @return a path over {@code writer}
     */
    public static <W, A> WriterPath<W, A> writer(Writer<W, A> writer, Monoid<W> monoid) {
        return new WriterPath<>(writer, monoid);
    }

    /**
     * Creates a writer path with empty output and a result value.
     *
     * @param <W> the output type
     * @param <A> the result type
     * @param value the result value
     * @param monoid the output monoid
     * @return a pure writer path
     */
    public static <W, A> WriterPath<W, A> writerPure(A value, Monoid<W> monoid) {
        return WriterPath.pure(value, monoid);
    }

    /**
     * Creates a writer path that emits output and returns {@link Unit}.
     *
     * @param <W> the output type
     * @param log the output to emit
     * @param monoid the output monoid
     * @return an output-emitting writer path
     */
    public static <W> WriterPath<W, Unit> tell(W log, Monoid<W> monoid) {
        return WriterPath.tell(log, monoid);
    }

    /**
     * Wraps a lazy value in a fluent path.
     *
     * @param <A> the value type
     * @param lazy the lazy value
     * @return a path over {@code lazy}
     */
    public static <A> LazyPath<A> lazy(Lazy<A> lazy) {
        return new LazyPath<>(lazy);
    }

    /**
     * Creates an already evaluated lazy path.
     *
     * @param <A> the value type
     * @param value the contained value
     * @return an evaluated lazy path
     */
    public static <A> LazyPath<A> lazyNow(A value) {
        return LazyPath.now(value);
    }

    /**
     * Creates a memoized lazy path from a supplier.
     *
     * @param <A> the value type
     * @param supplier the deferred computation
     * @return a deferred lazy path
     */
    public static <A> LazyPath<A> lazyDefer(Supplier<? extends A> supplier) {
        return LazyPath.defer(supplier);
    }

    /**
     * Wraps a completion stage in a fluent path.
     *
     * @param <A> the result type
     * @param future the completion stage
     * @return a path over {@code future}
     */
    public static <A> CompletableFuturePath<A> futurePath(CompletableFuture<A> future) {
        return CompletableFuturePath.fromFuture(future);
    }

    /**
     * Creates a completed future path.
     *
     * @param <A> the result type
     * @param value the completed value
     * @return a successfully completed future path
     */
    public static <A> CompletableFuturePath<A> futureCompleted(A value) {
        return CompletableFuturePath.completed(value);
    }

    /**
     * Creates a failed future path.
     *
     * @param <A> the result type
     * @param error the failure cause
     * @return an exceptionally completed future path
     */
    public static <A> CompletableFuturePath<A> futureFailed(Throwable error) {
        return CompletableFuturePath.failed(error);
    }

    /**
     * Creates a future path whose supplier runs asynchronously.
     *
     * @param <A> the result type
     * @param supplier the asynchronous computation
     * @return an asynchronously completed future path
     */
    public static <A> CompletableFuturePath<A> futureAsync(Supplier<A> supplier) {
        return CompletableFuturePath.supplyAsync(supplier);
    }

    /**
     * Wraps a Java stream in a fluent path.
     *
     * @param <A> the element type
     * @param stream the source stream
     * @return a path over {@code stream}
     */
    public static <A> StreamPath<A> stream(Stream<A> stream) {
        return new StreamPath<>(stream);
    }

    /**
     * Creates a stream path from list elements in encounter order.
     *
     * @param <A> the element type
     * @param list the source elements
     * @return a stream path over {@code list}
     */
    public static <A> StreamPath<A> streamFromList(List<A> list) {
        return new StreamPath<>(list.stream());
    }

    /**
     * Creates a singleton stream path.
     *
     * @param <A> the element type
     * @param value the sole element
     * @return a singleton stream path
     */
    public static <A> StreamPath<A> streamPure(A value) {
        return new StreamPath<>(Stream.of(value));
    }

    /**
     * Creates an empty stream path.
     *
     * @param <A> the element type
     * @return an empty stream path
     */
    public static <A> StreamPath<A> streamEmpty() {
        return new StreamPath<>(Stream.empty());
    }

    /**
     * Creates an unbounded iterative stream path.
     *
     * @param <A> the element type
     * @param seed the first element
     * @param mapper the function producing each subsequent element
     * @return an iterative stream path
     */
    public static <A> StreamPath<A> streamIterate(A seed, UnaryOperator<A> mapper) {
        return new StreamPath<>(Stream.iterate(seed, mapper));
    }

    /**
     * Wraps a Java stream as a reusable stream value.
     *
     * @param <A> the element type
     * @param stream the source stream
     * @return a stream value over {@code stream}
     */
    public static <A> StreamK<A> streamK(Stream<A> stream) {
        return StreamK.of(stream);
    }

    /**
     * Creates a reusable stream value from arguments in declaration order.
     *
     * @param <A> the element type
     * @param values the stream elements
     * @return a stream value containing {@code values}
     */
    @SafeVarargs
    public static <A> StreamK<A> streamKOf(A... values) {
        return StreamK.of(values);
    }

    /**
     * Creates a reusable stream value from encountered elements.
     *
     * @param <A> the element type
     * @param values the source elements
     * @return a stream value in encounter order
     */
    public static <A> StreamK<A> streamKFromIterable(Iterable<? extends A> values) {
        return StreamK.fromIterable(values);
    }

    /**
     * Creates a singleton reusable stream value.
     *
     * @param <A> the element type
     * @param value the sole element
     * @return a singleton stream value
     */
    public static <A> StreamK<A> streamKPure(A value) {
        return StreamK.pure(value);
    }

    /**
     * Creates an empty reusable stream value.
     *
     * @param <A> the element type
     * @return an empty stream value
     */
    public static <A> StreamK<A> streamKEmpty() {
        return StreamK.empty();
    }

    /**
     * Creates a reusable stream value from a deferred stream supplier.
     *
     * @param <A> the element type
     * @param supplier the supplier invoked for each stream materialization
     * @return a deferred stream value
     */
    public static <A> StreamK<A> streamKDefer(Supplier<? extends Stream<A>> supplier) {
        return StreamK.defer(supplier);
    }

    /**
     * Creates an unbounded iterative reusable stream value.
     *
     * @param <A> the element type
     * @param seed the first element
     * @param mapper the function producing each subsequent element
     * @return an iterative stream value
     */
    public static <A> StreamK<A> streamKIterate(A seed, UnaryOperator<A> mapper) {
        return StreamK.iterate(seed, mapper);
    }

    /**
     * Creates an unbounded generated reusable stream value.
     *
     * @param <A> the element type
     * @param supplier the element supplier
     * @return a generated stream value
     */
    public static <A> StreamK<A> streamKGenerate(Supplier<? extends A> supplier) {
        return StreamK.generate(supplier);
    }

    /**
     * Creates an integer stream value over a half-open range.
     *
     * @param startInclusive the first value
     * @param endExclusive the exclusive upper bound
     * @return a stream value containing the range
     */
    public static StreamK<Integer> streamKRange(int startInclusive, int endExclusive) {
        return StreamK.range(startInclusive, endExclusive);
    }

    /**
     * Creates an integer stream value over a closed range.
     *
     * @param startInclusive the first value
     * @param endInclusive the inclusive upper bound
     * @return a stream value containing the range
     */
    public static StreamK<Integer> streamKRangeClosed(int startInclusive, int endInclusive) {
        return StreamK.rangeClosed(startInclusive, endInclusive);
    }

    /**
     * Wraps a virtual-thread stream in a fluent path.
     *
     * @param <A> the element type
     * @param stream the virtual-thread stream
     * @return a path over {@code stream}
     */
    public static <A> VStreamPath<A> vstream(VStream<A> stream) {
        return new VStreamPath<>(stream);
    }

    /**
     * Creates a virtual-thread stream path from arguments in declaration order.
     *
     * @param <A> the element type
     * @param values the stream elements
     * @return a virtual-thread stream path containing {@code values}
     */
    @SafeVarargs
    public static <A> VStreamPath<A> vstreamOf(A... values) {
        return new VStreamPath<>(VStream.of(values));
    }

    /**
     * Creates a virtual-thread stream path from list elements.
     *
     * @param <A> the element type
     * @param list the source elements
     * @return a virtual-thread stream path in encounter order
     */
    public static <A> VStreamPath<A> vstreamFromList(List<A> list) {
        return new VStreamPath<>(VStream.fromList(list));
    }

    /**
     * Creates a singleton virtual-thread stream path.
     *
     * @param <A> the element type
     * @param value the sole element
     * @return a singleton virtual-thread stream path
     */
    public static <A> VStreamPath<A> vstreamPure(A value) {
        return new VStreamPath<>(VStream.of(value));
    }

    /**
     * Creates an empty virtual-thread stream path.
     *
     * @param <A> the element type
     * @return an empty virtual-thread stream path
     */
    public static <A> VStreamPath<A> vstreamEmpty() {
        return new VStreamPath<>(VStream.empty());
    }

    /**
     * Creates an unbounded iterative virtual-thread stream path.
     *
     * @param <A> the element type
     * @param seed the first element
     * @param mapper the function producing each subsequent element
     * @return an iterative virtual-thread stream path
     */
    public static <A> VStreamPath<A> vstreamIterate(A seed, UnaryOperator<A> mapper) {
        return new VStreamPath<>(VStream.iterate(seed, mapper));
    }

    /**
     * Creates an unbounded generated virtual-thread stream path.
     *
     * @param <A> the element type
     * @param supplier the element supplier
     * @return a generated virtual-thread stream path
     */
    public static <A> VStreamPath<A> vstreamGenerate(Supplier<A> supplier) {
        return new VStreamPath<>(VStream.generate(supplier));
    }

    /**
     * Creates an integer virtual-thread stream path over a half-open range.
     *
     * @param startInclusive the first value
     * @param endExclusive the exclusive upper bound
     * @return a virtual-thread stream path containing the range
     */
    public static VStreamPath<Integer> vstreamRange(int startInclusive, int endExclusive) {
        return new VStreamPath<>(VStream.range(startInclusive, endExclusive));
    }

    /**
     * Creates a virtual-thread stream path from a reactive publisher.
     *
     * @param <A> the element type
     * @param publisher the source publisher
     * @return a virtual-thread stream path over publisher signals
     */
    public static <A> VStreamPath<A> vstreamFromPublisher(Flow.Publisher<A> publisher) {
        return new VStreamPath<>(VStream.fromPublisher(publisher));
    }

    /**
     * Creates a virtual-thread stream path from a reactive publisher with bounded buffering.
     *
     * @param <A> the element type
     * @param publisher the source publisher
     * @param bufferSize the maximum number of buffered signals
     * @return a virtual-thread stream path over publisher signals
     * @throws IllegalArgumentException if {@code bufferSize} is not positive
     */
    public static <A> VStreamPath<A> vstreamFromPublisher(Flow.Publisher<A> publisher, int bufferSize) {
        return new VStreamPath<>(VStream.fromPublisher(publisher, bufferSize));
    }
}

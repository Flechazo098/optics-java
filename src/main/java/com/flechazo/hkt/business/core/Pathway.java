package com.flechazo.hkt.business.core;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.data.NonEmptyList;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.StreamPath;
import com.flechazo.hkt.business.stream.VStream;
import com.flechazo.hkt.business.stream.VStreamPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class Pathway {
    private Pathway() {
    }

    public static <A> MaybePath<A> just(A value) {
        return new MaybePath<>(Maybe.some(value));
    }

    public static <A> MaybePath<A> nothing() {
        return new MaybePath<>(Maybe.none());
    }

    public static <A> MaybePath<A> nullable(A value) {
        return new MaybePath<>(Maybe.ofNullable(value));
    }

    public static <A> MaybePath<A> maybe(Maybe<A> value) {
        return new MaybePath<>(value);
    }

    public static <A> MaybePath<A> optional(Optional<? extends A> value) {
        return new MaybePath<>(Maybe.fromOptional(value));
    }

    public static <E, A> EitherPath<E, A> right(A value) {
        return new EitherPath<>(Either.right(value));
    }

    public static <E, A> EitherPath<E, A> left(E error) {
        return new EitherPath<>(Either.left(error));
    }

    public static <E, A> EitherPath<E, A> either(Either<E, A> value) {
        return new EitherPath<>(value);
    }

    public static <A> TaskPath<A> task(CheckedSupplier<? extends A, ?> supplier) {
        return new TaskPath<>(Task.delay(supplier));
    }

    public static <A> TryPath<A> tryOf(CheckedSupplier<? extends A, ?> supplier) {
        return new TryPath<>(Try.of(supplier));
    }

    public static <A> TryPath<A> success(A value) {
        return new TryPath<>(Try.success(value));
    }

    public static <A> TryPath<A> failure(Throwable error) {
        return new TryPath<>(Try.failure(error));
    }

    public static <A> TryPath<A> tryPath(Try<A> value) {
        return new TryPath<>(value);
    }

    public static <A> IOPath<A> io(CheckedSupplier<? extends A, ?> supplier) {
        return new IOPath<>(IO.delay(supplier));
    }

    public static IOPath<Unit> ioRunnable(Runnable runnable) {
        return new IOPath<>(IO.exec(runnable));
    }

    public static <A> IOPath<A> ioPure(A value) {
        return new IOPath<>(IO.pure(value));
    }

    public static <A> IOPath<A> ioFail(Throwable error) {
        return new IOPath<>(IO.failed(error));
    }

    public static <A> IOPath<A> ioPath(IO<A> value) {
        return new IOPath<>(value);
    }

    public static TaskPath<Unit> task(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return new TaskPath<>(Task.delay(() -> {
            runnable.run();
            return Unit.INSTANCE;
        }));
    }

    public static <A> TaskPath<A> task(Task<A> value) {
        return new TaskPath<>(value);
    }

    public static <A> TaskPath<A> taskPath(Task<A> value) {
        return new TaskPath<>(value);
    }

    public static <A> TaskPath<A> future(Supplier<? extends CompletableFuture<A>> supplier) {
        return new TaskPath<>(Task.async(supplier));
    }

    public static <A> TaskPath<A> taskPure(A value) {
        return new TaskPath<>(Task.pure(value));
    }

    public static <A> TaskPath<A> taskFail(Throwable error) {
        return new TaskPath<>(Task.failed(error));
    }

    public static <E, A> Validated<E, A> valid(A value) {
        return Validated.valid(value);
    }

    public static <E, A> Validated<E, A> invalid(E error) {
        return Validated.invalid(error);
    }

    public static <E, A> ValidationPath<E, A> valid(A value, Semigroup<E> semigroup) {
        return new ValidationPath<>(Validated.valid(value), semigroup);
    }

    public static <E, A> ValidationPath<E, A> invalid(E error, Semigroup<E> semigroup) {
        return new ValidationPath<>(Validated.invalid(error), semigroup);
    }

    public static <E, A> ValidationPath<NonEmptyList<E>, A> validNel(A value) {
        return new ValidationPath<>(ValidatedNel.valid(value), NonEmptyList.semigroup());
    }

    public static <E, A> ValidationPath<NonEmptyList<E>, A> invalidNel(E error) {
        return new ValidationPath<>(ValidatedNel.invalid(error), NonEmptyList.semigroup());
    }

    public static <E> Semigroup<NonEmptyList<E>> nelSemigroup() {
        return NonEmptyList.semigroup();
    }

    public static <A> ListPath<A> listPath(List<? extends A> values) {
        return new ListPath<>(values);
    }

    @SafeVarargs
    public static <A> ListPath<A> listPath(A... values) {
        return new ListPath<>(List.of(values));
    }

    public static <A> ListPath<A> listPathPure(A value) {
        return new ListPath<>(List.of(value));
    }

    public static <A> ListPath<A> listPathEmpty() {
        return new ListPath<>(List.of());
    }

    public static ListPath<Integer> listPathRange(int startInclusive, int endExclusive) {
        ArrayList<Integer> values = new ArrayList<>(Math.max(endExclusive - startInclusive, 0));
        for (int i = startInclusive; i < endExclusive; i++) {
            values.add(i);
        }
        return new ListPath<>(values);
    }

    public static <R, A> ReaderPath<R, A> reader(Reader<R, A> reader) {
        return new ReaderPath<>(reader);
    }

    public static <R, A> ReaderPath<R, A> readerPure(A value) {
        return ReaderPath.pure(value);
    }

    public static <R> ReaderPath<R, R> ask() {
        return ReaderPath.ask();
    }

    public static <R, A> ReaderPath<R, A> asks(Function<? super R, ? extends A> mapper) {
        return ReaderPath.asks(mapper);
    }

    public static <S, A> WithStatePath<S, A> state(State<S, A> state) {
        return new WithStatePath<>(state);
    }

    public static <S, A> WithStatePath<S, A> statePure(A value) {
        return WithStatePath.pure(value);
    }

    public static <S> WithStatePath<S, S> getState() {
        return WithStatePath.get();
    }

    public static <S> WithStatePath<S, Unit> setState(S newState) {
        return WithStatePath.set(newState);
    }

    public static <S> WithStatePath<S, Unit> modifyState(UnaryOperator<S> mapper) {
        return WithStatePath.modify(mapper);
    }

    public static <W, A> WriterPath<W, A> writer(Writer<W, A> writer, Monoid<W> monoid) {
        return new WriterPath<>(writer, monoid);
    }

    public static <W, A> WriterPath<W, A> writerPure(A value, Monoid<W> monoid) {
        return WriterPath.pure(value, monoid);
    }

    public static <W> WriterPath<W, Unit> tell(W log, Monoid<W> monoid) {
        return WriterPath.tell(log, monoid);
    }

    public static <A> LazyPath<A> lazy(Lazy<A> lazy) {
        return new LazyPath<>(lazy);
    }

    public static <A> LazyPath<A> lazyNow(A value) {
        return LazyPath.now(value);
    }

    public static <A> LazyPath<A> lazyDefer(Supplier<? extends A> supplier) {
        return LazyPath.defer(supplier);
    }

    public static <A> CompletableFuturePath<A> futurePath(CompletableFuture<A> future) {
        return CompletableFuturePath.fromFuture(future);
    }

    public static <A> CompletableFuturePath<A> futureCompleted(A value) {
        return CompletableFuturePath.completed(value);
    }

    public static <A> CompletableFuturePath<A> futureFailed(Throwable error) {
        return CompletableFuturePath.failed(error);
    }

    public static <A> CompletableFuturePath<A> futureAsync(Supplier<A> supplier) {
        return CompletableFuturePath.supplyAsync(supplier);
    }

    public static <A> StreamPath<A> stream(Stream<A> stream) {
        return new StreamPath<>(stream);
    }

    public static <A> StreamPath<A> streamFromList(List<A> list) {
        return new StreamPath<>(list.stream());
    }

    public static <A> StreamPath<A> streamPure(A value) {
        return new StreamPath<>(Stream.of(value));
    }

    public static <A> StreamPath<A> streamEmpty() {
        return new StreamPath<>(Stream.empty());
    }

    public static <A> StreamPath<A> streamIterate(A seed, UnaryOperator<A> mapper) {
        return new StreamPath<>(Stream.iterate(seed, mapper));
    }

    public static <A> VStreamPath<A> vstream(VStream<A> stream) {
        return new VStreamPath<>(stream);
    }

    @SafeVarargs
    public static <A> VStreamPath<A> vstreamOf(A... values) {
        return new VStreamPath<>(VStream.of(values));
    }

    public static <A> VStreamPath<A> vstreamFromList(List<A> list) {
        return new VStreamPath<>(VStream.fromList(list));
    }

    public static <A> VStreamPath<A> vstreamPure(A value) {
        return new VStreamPath<>(VStream.of(value));
    }

    public static <A> VStreamPath<A> vstreamEmpty() {
        return new VStreamPath<>(VStream.empty());
    }

    public static <A> VStreamPath<A> vstreamIterate(A seed, UnaryOperator<A> mapper) {
        return new VStreamPath<>(VStream.iterate(seed, mapper));
    }

    public static <A> VStreamPath<A> vstreamGenerate(Supplier<A> supplier) {
        return new VStreamPath<>(VStream.generate(supplier));
    }

    public static VStreamPath<Integer> vstreamRange(int startInclusive, int endExclusive) {
        return new VStreamPath<>(VStream.range(startInclusive, endExclusive));
    }

    public static <A> VStreamPath<A> vstreamFromPublisher(Flow.Publisher<A> publisher) {
        return new VStreamPath<>(VStream.fromPublisher(publisher));
    }
}

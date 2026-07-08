package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.stream.internal.*;
import com.flechazo.hkt.util.validation.Validation;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@FunctionalInterface
public interface VStream<A> extends App<VStream.Mu, A> {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    final class InstanceMu implements MonadError.Mu, MonadZero.Mu, Traversable.Mu {
        private InstanceMu() {
        }
    }

    sealed interface Step<A> permits Emit, Done, Skip {
    }

    record Emit<A>(A value, VStream<A> tail) implements Step<A> {
        public Emit {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(tail, "tail");
        }
    }

    record Done<A>() implements Step<A> {
    }

    record Skip<A>(VStream<A> tail) implements Step<A> {
        public Skip {
            Objects.requireNonNull(tail, "tail");
        }
    }

    record Seed<A, S>(A value, S next) {
        public Seed {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(next, "next");
        }
    }

    Task<Step<A>> pull();

    static <A> VStream<A> unbox(App<Mu, A> value) {
        return (VStream<A>) Validation.kind().narrowWithTypeCheck(value, VStream.class);
    }

    static Functor<VStream.Mu, InstanceMu> functor() {
        return VStreamFunctor.INSTANCE;
    }

    static Applicative<VStream.Mu, InstanceMu> applicative() {
        return VStreamApplicative.INSTANCE;
    }

    static Monad<VStream.Mu, InstanceMu> monad() {
        return VStreamMonad.INSTANCE;
    }

    static MonadZero<VStream.Mu, InstanceMu> monadZero() {
        return VStreamAlternative.INSTANCE;
    }

    static MonadError<VStream.Mu, Throwable, InstanceMu> monadError() {
        return VStreamAlternative.INSTANCE;
    }

    static Selective<VStream.Mu, InstanceMu> selective() {
        return VStreamAlternative.INSTANCE;
    }

    static Foldable<VStream.Mu> foldable() {
        return VStreamTraverse.INSTANCE;
    }

    static Traversable<VStream.Mu, InstanceMu> traversable() {
        return VStreamTraverse.INSTANCE;
    }

    default Task<Unit> close() {
        return Task.unit();
    }

    static <A> VStream<A> empty() {
        return () -> Task.pure(new Done<>());
    }

    static <A> VStream<A> of(A value) {
        return () -> Task.pure(new Emit<>(value, empty()));
    }

    @SafeVarargs
    static <A> VStream<A> of(A... values) {
        return fromList(List.of(values));
    }

    static <A> VStream<A> fromList(List<A> list) {
        return fromListAt(list, 0);
    }

    static <A> VStream<A> fromStream(Stream<A> stream) {
        return fromIterator(stream.iterator()).onFinalize(Task.delay(() -> {
            stream.close();
            return Unit.INSTANCE;
        }));
    }

    static <A> VStream<A> fromIterator(Iterator<A> iterator) {
        return () -> Task.delay(() -> iterator.hasNext()
                ? new Emit<>(iterator.next(), fromIterator(iterator))
                : new Done<>());
    }

    static <A> VStream<A> succeed(A value) {
        return of(value);
    }

    static <A> VStream<A> fail(Throwable error) {
        return () -> Task.failed(error);
    }

    static <A> VStream<A> iterate(A seed, UnaryOperator<A> mapper) {
        return () -> Task.pure(new Emit<>(seed, iterate(mapper.apply(seed), mapper)));
    }

    static <S, A> VStream<A> unfold(S initialState, Function<? super S, Task<Maybe<Seed<A, S>>>> f) {
        return () -> f.apply(initialState).map(maybe -> maybe
                .<Step<A>>map(seed -> new Emit<>(seed.value(), unfold(seed.next(), f)))
                .orElseGet(Done::new));
    }

    static <A> VStream<A> generate(Supplier<A> supplier) {
        return () -> Task.delay(() -> new Emit<>(supplier.get(), generate(supplier)));
    }

    static <A> VStream<A> concat(VStream<A> first, VStream<A> second) {
        return () -> first.pull().map(step -> switch (step) {
            case Emit<A> emit -> new Emit<>(emit.value(), concat(emit.tail(), second));
            case Skip<A> skip -> new Skip<>(concat(skip.tail(), second));
            case Done<A> ignored -> new Skip<>(second);
        });
    }

    static <A> VStream<A> repeat(A value) {
        return () -> Task.pure(new Emit<>(value, repeat(value)));
    }

    static VStream<Integer> range(int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }
        return () -> Task.pure(new Emit<>(startInclusive, range(startInclusive + 1, endExclusive)));
    }

    static <A> VStream<A> defer(Supplier<VStream<A>> supplier) {
        return () -> supplier.get().pull();
    }

    static <R, A> VStream<A> bracket(
            Task<R> acquire,
            Function<? super R, ? extends VStream<A>> use,
            Function<? super R, Task<Unit>> release) {
        return () -> acquire.flatMap(resource -> use.apply(resource).onFinalize(release.apply(resource)).pull());
    }

    static <A> VStream<A> fromPublisher(Flow.Publisher<A> publisher) {
        return VStreamReactive.fromPublisher(publisher);
    }

    static <A> VStream<A> fromPublisher(Flow.Publisher<A> publisher, int bufferSize) {
        return VStreamReactive.fromPublisher(publisher, bufferSize);
    }

    default <B> VStream<B> map(Function<? super A, ? extends B> mapper) {
        return new MappedStream<>(this, mapper);
    }

    default <B> VStream<B> flatMap(Function<? super A, ? extends VStream<B>> mapper) {
        return new FlatMapStream<>(this, mapper);
    }

    default <B> VStream<B> via(Function<? super A, ? extends VStream<B>> mapper) {
        return flatMap(mapper);
    }

    default <B> VStream<B> mapTask(Function<? super A, ? extends Task<B>> mapper) {
        return () -> pull().flatMap(step -> switch (step) {
            case Emit<A> emit -> {
                VStream<B> tail = emit.tail().mapTask(mapper);
                yield mapper.apply(emit.value())
                        .<Step<B>>map(value -> new Emit<>(value, tail))
                        .recoverWith(error -> {
                            error.addSuppressed(new StreamTailMarker(tail));
                            return Task.failed(error);
                        });
            }
            case Skip<A> skip -> Task.pure(new Skip<>(skip.tail().mapTask(mapper)));
            case Done<A> ignored -> Task.pure(new Done<>());
        });
    }

    default <B> VStream<B> parEvalMap(int concurrency, Function<? super A, ? extends Task<B>> mapper) {
        return VStreamPar.parEvalMap(this, concurrency, mapper);
    }

    default <B> VStream<B> parEvalMapUnordered(int concurrency, Function<? super A, ? extends Task<B>> mapper) {
        return VStreamPar.parEvalMapUnordered(this, concurrency, mapper);
    }

    default <B> VStream<B> parEvalFlatMap(int concurrency, Function<? super A, ? extends VStream<B>> mapper) {
        return VStreamPar.parEvalFlatMap(this, concurrency, mapper);
    }

    default VStream<A> filter(Predicate<? super A> predicate) {
        return () -> pull().map(step -> switch (step) {
            case Emit<A> emit -> predicate.test(emit.value())
                    ? new Emit<>(emit.value(), emit.tail().filter(predicate))
                    : new Skip<>(emit.tail().filter(predicate));
            case Skip<A> skip -> new Skip<>(skip.tail().filter(predicate));
            case Done<A> ignored -> new Done<>();
        });
    }

    default VStream<A> takeWhile(Predicate<? super A> predicate) {
        return () -> pull().flatMap(step -> switch (step) {
            case Emit<A> emit -> predicate.test(emit.value())
                    ? Task.pure(new Emit<>(emit.value(), emit.tail().takeWhile(predicate)))
                    : emit.tail().close().map(ignored -> new Done<>());
            case Skip<A> skip -> Task.pure(new Skip<>(skip.tail().takeWhile(predicate)));
            case Done<A> ignored -> Task.pure(new Done<>());
        });
    }

    default VStream<A> dropWhile(Predicate<? super A> predicate) {
        return defer(() -> {
            VStream<A> current = this;
            while (true) {
                Step<A> step = current.pull().unsafeRun();
                switch (step) {
                    case Emit<A> emit -> {
                        if (predicate.test(emit.value())) {
                            current = emit.tail();
                        } else {
                            return concat(of(emit.value()), emit.tail());
                        }
                    }
                    case Skip<A> skip -> current = skip.tail();
                    case Done<A> ignored -> {
                        return empty();
                    }
                }
            }
        });
    }

    default VStream<A> take(long n) {
        if (n <= 0) {
            return () -> close().map(ignored -> new Done<>());
        }
        return () -> pull().map(step -> switch (step) {
            case Emit<A> emit -> new Emit<>(emit.value(), emit.tail().take(n - 1));
            case Skip<A> skip -> new Skip<>(skip.tail().take(n));
            case Done<A> ignored -> new Done<>();
        });
    }

    default VStream<A> drop(long n) {
        if (n <= 0) {
            return this;
        }
        return () -> pull().map(step -> switch (step) {
            case Emit<A> emit -> new Skip<>(emit.tail().drop(n - 1));
            case Skip<A> skip -> new Skip<>(skip.tail().drop(n));
            case Done<A> ignored -> new Done<>();
        });
    }

    default VStream<A> distinct() {
        HashSet<A> seen = new HashSet<>();
        return filter(seen::add);
    }

    default VStream<List<A>> chunk(int size) {
        return defer(() -> {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be positive");
            }
            ArrayList<A> chunk = new ArrayList<>(size);
            VStream<A> current = this;
            while (chunk.size() < size) {
                Step<A> step = current.pull().unsafeRun();
                switch (step) {
                    case Emit<A> emit -> {
                        chunk.add(emit.value());
                        current = emit.tail();
                    }
                    case Skip<A> skip -> current = skip.tail();
                    case Done<A> ignored -> {
                        return chunk.isEmpty() ? empty() : of(Collections.unmodifiableList(chunk));
                    }
                }
            }
            return concat(of(Collections.unmodifiableList(chunk)), current.chunk(size));
        });
    }

    default VStream<List<A>> chunkWhile(BiPredicate<? super A, ? super A> sameChunk) {
        return defer(() -> {
            VStream<A> current = this;
            while (true) {
                Step<A> step = current.pull().unsafeRun();
                switch (step) {
                    case Emit<A> emit -> {
                        ArrayList<A> chunk = new ArrayList<>();
                        chunk.add(emit.value());
                        return buildChunkWhile(chunk, emit.value(), emit.tail(), sameChunk);
                    }
                    case Skip<A> skip -> current = skip.tail();
                    case Done<A> ignored -> {
                        return empty();
                    }
                }
            }
        });
    }

    default <B> VStream<B> mapChunked(int size, Function<? super List<A>, ? extends List<B>> mapper) {
        return chunk(size).flatMap(list -> fromList(mapper.apply(list)));
    }

    default VStream<A> concat(VStream<A> other) {
        return concat(this, other);
    }

    default VStream<A> prepend(A value) {
        return concat(of(value), this);
    }

    default VStream<A> append(A value) {
        return concat(of(value));
    }

    default <B, C> VStream<C> zipWith(VStream<B> other, BiFunction<? super A, ? super B, ? extends C> combiner) {
        return new ZipWithStream<>(this, other, combiner);
    }

    default VStream<A> interleave(VStream<A> other) {
        return new InterleaveStream<>(this, other);
    }

    default VStream<A> merge(VStream<A> other) {
        return VStreamPar.merge(this, other);
    }

    default Flow.Publisher<A> toPublisher() {
        return VStreamReactive.toPublisher(this);
    }

    default VStream<A> throttle(int maxElements, Duration window) {
        return VStreamThrottle.throttle(this, maxElements, window);
    }

    default VStream<A> metered(Duration interval) {
        return VStreamThrottle.metered(this, interval);
    }

    default VStream<A> peek(Consumer<? super A> action) {
        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    default VStream<A> onComplete(Runnable action) {
        return onFinalize(Task.delay(() -> {
            action.run();
            return Unit.INSTANCE;
        }));
    }

    default Task<List<A>> toList() {
        return Task.delay(() -> {
            ArrayList<A> result = new ArrayList<>();
            VStream<A> current = this;
            try {
                while (true) {
                    Step<A> step = current.pull().unsafeRun();
                    switch (step) {
                        case Emit<A> emit -> {
                            result.add(emit.value());
                            current = emit.tail();
                        }
                        case Skip<A> skip -> current = skip.tail();
                        case Done<A> ignored -> {
                            return result;
                        }
                    }
                }
            } finally {
                current.close().unsafeRun();
            }
        });
    }

    default Task<List<A>> parCollect(int batchSize) {
        return VStreamPar.parCollect(this, batchSize);
    }

    default Task<A> fold(A identity, BinaryOperator<A> op) {
        return foldLeft(identity, op);
    }

    default <B> Task<B> foldLeft(B initial, BiFunction<B, A, B> f) {
        return Task.delay(() -> {
            B result = initial;
            VStream<A> current = this;
            try {
                while (true) {
                    Step<A> step = current.pull().unsafeRun();
                    switch (step) {
                        case Emit<A> emit -> {
                            result = f.apply(result, emit.value());
                            current = emit.tail();
                        }
                        case Skip<A> skip -> current = skip.tail();
                        case Done<A> ignored -> {
                            return result;
                        }
                    }
                }
            } finally {
                current.close().unsafeRun();
            }
        });
    }

    default Task<Maybe<A>> head() {
        return Task.delay(() -> {
            VStream<A> current = this;
            try {
                while (true) {
                    Step<A> step = current.pull().unsafeRun();
                    switch (step) {
                        case Emit<A> emit -> {
                            current = emit.tail();
                            return Maybe.some(emit.value());
                        }
                        case Skip<A> skip -> current = skip.tail();
                        case Done<A> ignored -> {
                            return Maybe.none();
                        }
                    }
                }
            } finally {
                current.close().unsafeRun();
            }
        });
    }

    default Task<Maybe<A>> last() {
        return Task.delay(() -> {
            Maybe<A> result = Maybe.none();
            VStream<A> current = this;
            try {
                while (true) {
                    Step<A> step = current.pull().unsafeRun();
                    switch (step) {
                        case Emit<A> emit -> {
                            result = Maybe.some(emit.value());
                            current = emit.tail();
                        }
                        case Skip<A> skip -> current = skip.tail();
                        case Done<A> ignored -> {
                            return result;
                        }
                    }
                }
            } finally {
                current.close().unsafeRun();
            }
        });
    }

    default Task<Long> count() {
        return foldLeft(0L, (count, ignored) -> count + 1);
    }

    default Task<Boolean> exists(Predicate<? super A> predicate) {
        return find(predicate).map(Maybe::isDefined);
    }

    default Task<Boolean> forAll(Predicate<? super A> predicate) {
        return Task.delay(() -> {
            VStream<A> current = this;
            try {
                while (true) {
                    Step<A> step = current.pull().unsafeRun();
                    switch (step) {
                        case Emit<A> emit -> {
                            if (!predicate.test(emit.value())) {
                                current = emit.tail();
                                return false;
                            }
                            current = emit.tail();
                        }
                        case Skip<A> skip -> current = skip.tail();
                        case Done<A> ignored -> {
                            return true;
                        }
                    }
                }
            } finally {
                current.close().unsafeRun();
            }
        });
    }

    default Task<Maybe<A>> find(Predicate<? super A> predicate) {
        return filter(predicate).head();
    }

    default Task<Unit> forEach(Consumer<? super A> consumer) {
        return Task.delay(() -> {
            VStream<A> current = this;
            try {
                while (true) {
                    Step<A> step = current.pull().unsafeRun();
                    switch (step) {
                        case Emit<A> emit -> {
                            consumer.accept(emit.value());
                            current = emit.tail();
                        }
                        case Skip<A> skip -> current = skip.tail();
                        case Done<A> ignored -> {
                            return Unit.INSTANCE;
                        }
                    }
                }
            } finally {
                current.close().unsafeRun();
            }
        });
    }

    default Task<Unit> drain() {
        return forEach(ignored -> {
        });
    }

    default VStream<Unit> asUnit() {
        return map(ignored -> Unit.INSTANCE);
    }

    default VStream<A> recover(Function<? super Throwable, ? extends A> recovery) {
        Function<Step<A>, Step<A>> recoverTail = step -> switch (step) {
            case Emit<A> emit -> new Emit<>(emit.value(), emit.tail().recover(recovery));
            case Skip<A> skip -> new Skip<>(skip.tail().recover(recovery));
            case Done<A> done -> done;
        };
        return () -> pull()
                .map(recoverTail)
                .recover(error -> {
                    VStream<A> tail = empty();
                    for (Throwable suppressed : error.getSuppressed()) {
                        if (suppressed instanceof StreamTailMarker marker) {
                            @SuppressWarnings("unchecked")
                            VStream<A> remaining = (VStream<A>) marker.remainingTail();
                            tail = remaining.recover(recovery);
                            break;
                        }
                    }
                    return new Emit<>(recovery.apply(error), tail);
                });
    }

    default VStream<A> recoverWith(Function<? super Throwable, ? extends VStream<A>> recovery) {
        return () -> pull().recoverWith(error -> recovery.apply(error).pull());
    }

    default VStream<A> mapError(Function<? super Throwable, ? extends Throwable> mapper) {
        return () -> pull().mapError(mapper);
    }

    default VStream<A> onError(Consumer<? super Throwable> action) {
        return () -> pull().mapError(error -> {
            action.accept(error);
            return error;
        });
    }

    default VStream<A> onFinalize(Task<Unit> finalizer) {
        AtomicBoolean released = new AtomicBoolean(false);
        return wrapWithFinalizer(this, finalizer, released);
    }

    default Task<Try<List<A>>> runSafe() {
        return toList().attempt().map(either -> either.fold(Try::failure, Try::success));
    }

    default CompletableFuture<List<A>> runAsync() {
        return toList().runAsync();
    }

    default Stream<A> toStream() {
        Iterator<A> iterator = new Iterator<>() {
            private VStream<A> current = VStream.this;
            private Step<A> next;
            private boolean loaded;

            @Override
            public boolean hasNext() {
                load();
                return next instanceof Emit<A>;
            }

            @Override
            public A next() {
                load();
                A value = ((Emit<A>) next).value();
                current = ((Emit<A>) next).tail();
                loaded = false;
                return value;
            }

            private void load() {
                if (loaded) {
                    return;
                }
                while (true) {
                    next = current.pull().unsafeRun();
                    if (next instanceof Skip<A>(VStream<A> tail)) {
                        current = tail;
                        continue;
                    }
                    if (next instanceof Done<A>) {
                        current.close().unsafeRun();
                    }
                    loaded = true;
                    return;
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                .onClose(() -> close().unsafeRun());
    }

    private static <A> VStream<A> fromListAt(List<A> list, int index) {
        if (index >= list.size()) {
            return empty();
        }
        return () -> Task.pure(new Emit<>(list.get(index), fromListAt(list, index + 1)));
    }

    private static <A> VStream<List<A>> buildChunkWhile(
            List<A> chunk,
            A previous,
            VStream<A> tail,
            BiPredicate<? super A, ? super A> sameChunk) {
        return defer(() -> {
            VStream<A> current = tail;
            List<A> currentChunk = chunk;
            A currentPrevious = previous;
            while (true) {
                Step<A> step = current.pull().unsafeRun();
                switch (step) {
                    case Emit<A> emit -> {
                        if (sameChunk.test(currentPrevious, emit.value())) {
                            currentChunk.add(emit.value());
                            currentPrevious = emit.value();
                            current = emit.tail();
                        } else {
                            List<A> emitted = Collections.unmodifiableList(currentChunk);
                            ArrayList<A> nextChunk = new ArrayList<>();
                            nextChunk.add(emit.value());
                            return concat(of(emitted), buildChunkWhile(nextChunk, emit.value(), emit.tail(), sameChunk));
                        }
                    }
                    case Skip<A> skip -> current = skip.tail();
                    case Done<A> ignored -> {
                        return of(Collections.unmodifiableList(currentChunk));
                    }
                }
            }
        });
    }

    private static <A> VStream<A> wrapWithFinalizer(VStream<A> source, Task<Unit> finalizer, AtomicBoolean released) {
        return new VStream<>() {
            @Override
            public Task<Step<A>> pull() {
                return source.pull()
                        .<Step<A>>map(step -> switch (step) {
                            case Emit<A> emit ->
                                    new Emit<>(emit.value(), wrapWithFinalizer(emit.tail(), finalizer, released));
                            case Skip<A> skip -> new Skip<>(wrapWithFinalizer(skip.tail(), finalizer, released));
                            case Done<A> ignored -> {
                                if (released.compareAndSet(false, true)) {
                                    finalizer.unsafeRun();
                                }
                                yield new Done<>();
                            }
                        })
                        .recoverWith(error -> {
                            if (released.compareAndSet(false, true)) {
                                try {
                                    finalizer.unsafeRun();
                                } catch (Throwable finalizerError) {
                                    error.addSuppressed(finalizerError);
                                }
                            }
                            return Task.failed(error);
                        });
            }

            @Override
            public Task<Unit> close() {
                return Task.delay(() -> {
                    if (released.compareAndSet(false, true)) {
                        finalizer.unsafeRun();
                    }
                    source.close().unsafeRun();
                    return Unit.INSTANCE;
                });
            }
        };
    }
}

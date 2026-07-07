package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

public sealed interface ScopeJoiner<T, R>
        permits ScopeJoiner.AllSucceed, ScopeJoiner.AnySucceed, ScopeJoiner.FirstComplete, ScopeJoiner.Accumulating {
    StructuredTaskScope.Joiner<T, R> joiner();

    default Either<Throwable, R> resultEither() {
        try {
            return Either.right(joiner().result());
        } catch (Throwable throwable) {
            return Either.left(throwable);
        }
    }

    static <T> ScopeJoiner<T, List<T>> allSucceed() {
        return new AllSucceed<>();
    }

    static <T> ScopeJoiner<T, T> anySucceed() {
        return new AnySucceed<>();
    }

    static <T> ScopeJoiner<T, T> firstComplete() {
        return new FirstComplete<>();
    }

    static <E, T> ScopeJoiner<T, Validated<List<E>, List<T>>> accumulating(
            Function<? super Throwable, ? extends E> errorMapper) {
        return new Accumulating<>(errorMapper);
    }

    @SuppressWarnings("preview")
    final class AllSucceed<T> implements ScopeJoiner<T, List<T>> {
        private final StructuredTaskScope.Joiner<T, List<T>> delegate;

        AllSucceed() {
            StructuredTaskScope.Joiner<T, Stream<StructuredTaskScope.Subtask<T>>> builtIn =
                    StructuredTaskScope.Joiner.allSuccessfulOrThrow();
            this.delegate = new StructuredTaskScope.Joiner<>() {
                @Override
                public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
                    return builtIn.onFork(subtask);
                }

                @Override
                public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
                    return builtIn.onComplete(subtask);
                }

                @Override
                public List<T> result() throws Throwable {
                    return builtIn.result().map(StructuredTaskScope.Subtask::get).toList();
                }
            };
        }

        @Override
        public StructuredTaskScope.Joiner<T, List<T>> joiner() {
            return delegate;
        }
    }

    @SuppressWarnings("preview")
    final class AnySucceed<T> implements ScopeJoiner<T, T> {
        private final StructuredTaskScope.Joiner<T, T> delegate =
                StructuredTaskScope.Joiner.anySuccessfulResultOrThrow();

        @Override
        public StructuredTaskScope.Joiner<T, T> joiner() {
            return delegate;
        }
    }

    @SuppressWarnings("preview")
    final class FirstComplete<T> implements ScopeJoiner<T, T> {
        private final StructuredTaskScope.Joiner<T, T> delegate;

        FirstComplete() {
            AtomicReference<StructuredTaskScope.Subtask<? extends T>> firstCompleted = new AtomicReference<>();
            StructuredTaskScope.Joiner<T, Void> completionTracker = StructuredTaskScope.Joiner.awaitAll();
            this.delegate = new StructuredTaskScope.Joiner<>() {
                @Override
                public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
                    return firstCompleted.get() == null && completionTracker.onFork(subtask);
                }

                @Override
                public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
                    if (firstCompleted.compareAndSet(null, subtask)) {
                        return false;
                    }
                    return completionTracker.onComplete(subtask);
                }

                @Override
                public T result() throws Throwable {
                    StructuredTaskScope.Subtask<? extends T> subtask = firstCompleted.get();
                    if (subtask == null) {
                        throw new IllegalStateException("No subtask completed");
                    }
                    if (subtask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                        throw subtask.exception();
                    }
                    return subtask.get();
                }
            };
        }

        @Override
        public StructuredTaskScope.Joiner<T, T> joiner() {
            return delegate;
        }
    }

    @SuppressWarnings("preview")
    final class Accumulating<E, T> implements ScopeJoiner<T, Validated<List<E>, List<T>>> {
        private final List<StructuredTaskScope.Subtask<? extends T>> allSubtasks =
                Collections.synchronizedList(new ArrayList<>());
        private final StructuredTaskScope.Joiner<T, Validated<List<E>, List<T>>> delegate;

        Accumulating(Function<? super Throwable, ? extends E> errorMapper) {
            Objects.requireNonNull(errorMapper, "errorMapper");
            StructuredTaskScope.Joiner<T, Void> completionTracker = StructuredTaskScope.Joiner.awaitAll();
            this.delegate = new StructuredTaskScope.Joiner<>() {
                @Override
                public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
                    allSubtasks.add(subtask);
                    return completionTracker.onFork(subtask);
                }

                @Override
                public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
                    return completionTracker.onComplete(subtask);
                }

                @Override
                public Validated<List<E>, List<T>> result() throws Throwable {
                    completionTracker.result();
                    ArrayList<E> errors = new ArrayList<>();
                    ArrayList<T> successes = new ArrayList<>();
                    for (var subtask : allSubtasks) {
                        if (subtask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                            errors.add(Objects.requireNonNull(errorMapper.apply(subtask.exception()), "mapped error"));
                        } else {
                            successes.add(subtask.get());
                        }
                    }
                    return errors.isEmpty() ? Validated.valid(successes) : Validated.invalid(errors);
                }
            };
        }

        @Override
        public StructuredTaskScope.Joiner<T, Validated<List<E>, List<T>>> joiner() {
            return delegate;
        }
    }
}

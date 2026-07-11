package com.flechazo.hkt.business.effect;

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
    // JDK joiners are stateful and single-use: every call must return a fresh
    // instance so the Task returned by Scope.join() can be executed repeatedly.
    StructuredTaskScope.Joiner<T, R> joiner();

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
        @Override
        public StructuredTaskScope.Joiner<T, List<T>> joiner() {
            StructuredTaskScope.Joiner<T, Stream<StructuredTaskScope.Subtask<T>>> builtIn =
                    StructuredTaskScope.Joiner.allSuccessfulOrThrow();
            return new StructuredTaskScope.Joiner<>() {
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
    }

    @SuppressWarnings("preview")
    final class AnySucceed<T> implements ScopeJoiner<T, T> {
        @Override
        public StructuredTaskScope.Joiner<T, T> joiner() {
            return StructuredTaskScope.Joiner.anySuccessfulResultOrThrow();
        }
    }

    @SuppressWarnings("preview")
    final class FirstComplete<T> implements ScopeJoiner<T, T> {
        @Override
        public StructuredTaskScope.Joiner<T, T> joiner() {
            AtomicReference<StructuredTaskScope.Subtask<? extends T>> firstCompleted = new AtomicReference<>();
            return new StructuredTaskScope.Joiner<>() {
                @Override
                public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
                    return firstCompleted.get() != null;
                }

                @Override
                public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
                    // The first completion (success or failure) wins; returning true
                    // cancels the scope so the losing subtasks are interrupted.
                    firstCompleted.compareAndSet(null, subtask);
                    return true;
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
    }

    @SuppressWarnings("preview")
    final class Accumulating<E, T> implements ScopeJoiner<T, Validated<List<E>, List<T>>> {
        private final Function<? super Throwable, ? extends E> errorMapper;

        Accumulating(Function<? super Throwable, ? extends E> errorMapper) {
            this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper");
        }

        @Override
        public StructuredTaskScope.Joiner<T, Validated<List<E>, List<T>>> joiner() {
            List<StructuredTaskScope.Subtask<? extends T>> allSubtasks =
                    Collections.synchronizedList(new ArrayList<>());
            StructuredTaskScope.Joiner<T, Void> completionTracker = StructuredTaskScope.Joiner.awaitAll();
            return new StructuredTaskScope.Joiner<>() {
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
                    for (StructuredTaskScope.Subtask<? extends T> subtask : allSubtasks) {
                        if (subtask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                            errors.add(Objects.requireNonNull(
                                    errorMapper.apply(subtask.exception()), "mapped error"));
                        } else {
                            successes.add(subtask.get());
                        }
                    }
                    return errors.isEmpty() ? Validated.valid(successes) : Validated.invalid(errors);
                }
            };
        }
    }
}

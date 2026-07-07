package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;

public final class Scope<T, R> {
    private final ScopeJoiner<T, R> joiner;
    private final List<Task<? extends T>> tasks;
    private final Duration timeout;
    private final String name;

    private Scope(ScopeJoiner<T, R> joiner, List<Task<? extends T>> tasks, Duration timeout, String name) {
        this.joiner = joiner;
        this.tasks = tasks;
        this.timeout = timeout;
        this.name = name;
    }

    public static <T> Scope<T, List<T>> allSucceed() {
        return new Scope<>(ScopeJoiner.allSucceed(), new ArrayList<>(), null, null);
    }

    public static <T> Scope<T, T> anySucceed() {
        return new Scope<>(ScopeJoiner.anySucceed(), new ArrayList<>(), null, null);
    }

    public static <T> Scope<T, T> firstComplete() {
        return new Scope<>(ScopeJoiner.firstComplete(), new ArrayList<>(), null, null);
    }

    public static <E, T> Scope<T, Validated<List<E>, List<T>>> accumulating(
            Function<? super Throwable, ? extends E> errorMapper) {
        return new Scope<>(ScopeJoiner.accumulating(errorMapper), new ArrayList<>(), null, null);
    }

    public static <T, R> Scope<T, R> withJoiner(ScopeJoiner<T, R> joiner) {
        return new Scope<>(Objects.requireNonNull(joiner, "joiner"), new ArrayList<>(), null, null);
    }

    public Scope<T, R> timeout(Duration timeout) {
        return new Scope<>(joiner, tasks, Objects.requireNonNull(timeout, "timeout"), name);
    }

    public Scope<T, R> named(String name) {
        return new Scope<>(joiner, tasks, timeout, name);
    }

    public Scope<T, R> fork(Task<? extends T> task) {
        Objects.requireNonNull(task, "task");
        ArrayList<Task<? extends T>> next = new ArrayList<>(tasks);
        next.add(task);
        return new Scope<>(joiner, next, timeout, name);
    }

    public Scope<T, R> forkAll(List<? extends Task<? extends T>> tasksToFork) {
        Objects.requireNonNull(tasksToFork, "tasksToFork");
        ArrayList<Task<? extends T>> next = new ArrayList<>(tasks);
        next.addAll(tasksToFork);
        return new Scope<>(joiner, next, timeout, name);
    }

    @SuppressWarnings("preview")
    public Task<R> join() {
        Task<R> joined = () -> {
            try (var scope = StructuredTaskScope.open(joiner.joiner())) {
                for (Task<? extends T> task : tasks) {
                    scope.fork(task.asCallable());
                }
                return scope.join();
            } catch (StructuredTaskScope.FailedException failed) {
                throw failed.getCause();
            }
        };
        return timeout != null ? joined.timeout(timeout) : joined;
    }

    public Task<Try<R>> joinSafe() {
        return join().map(Try::success).recover(Try::failure);
    }

    public Task<Either<Throwable, R>> joinEither() {
        return join().map(Either::<Throwable, R>right).recover(Either::left);
    }

    public Task<Maybe<R>> joinMaybe() {
        return join().map(Maybe::some).recover(error -> Maybe.none());
    }

    public int taskCount() {
        return tasks.size();
    }

    public boolean hasTimeout() {
        return timeout != null;
    }

    public Maybe<Duration> getTimeout() {
        return timeout != null ? Maybe.some(timeout) : Maybe.none();
    }

    public Maybe<String> name() {
        return name != null ? Maybe.some(name) : Maybe.none();
    }
}

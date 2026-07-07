package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.Tuple3;
import com.flechazo.hkt.function.Function3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Par {
    private Par() {
    }

    @SuppressWarnings("preview")
    public static <A, B> Task<Tuple2<A, B>> zip(Task<A> first, Task<B> second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return () -> {
            try (var scope = StructuredTaskScope.open()) {
                var firstTask = scope.fork(first.asCallable());
                var secondTask = scope.fork(second.asCallable());
                scope.join();
                return new Tuple2<>(firstTask.get(), secondTask.get());
            } catch (StructuredTaskScope.FailedException failed) {
                throw failed.getCause();
            }
        };
    }

    @SuppressWarnings("preview")
    public static <A, B, C> Task<Tuple3<A, B, C>> zip3(Task<A> first, Task<B> second, Task<C> third) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        return () -> {
            try (var scope = StructuredTaskScope.open()) {
                var firstTask = scope.fork(first.asCallable());
                var secondTask = scope.fork(second.asCallable());
                var thirdTask = scope.fork(third.asCallable());
                scope.join();
                return new Tuple3<>(firstTask.get(), secondTask.get(), thirdTask.get());
            } catch (StructuredTaskScope.FailedException failed) {
                throw failed.getCause();
            }
        };
    }

    @SuppressWarnings("preview")
    public static <A, B, R> Task<R> map2(
            Task<A> first,
            Task<B> second,
            BiFunction<? super A, ? super B, ? extends R> combiner) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(combiner, "combiner");
        return () -> {
            try (var scope = StructuredTaskScope.open()) {
                var firstTask = scope.fork(first.asCallable());
                var secondTask = scope.fork(second.asCallable());
                scope.join();
                return Objects.requireNonNull(combiner.apply(firstTask.get(), secondTask.get()), "map2 result");
            } catch (StructuredTaskScope.FailedException failed) {
                throw failed.getCause();
            }
        };
    }

    @SuppressWarnings("preview")
    public static <A, B, C, R> Task<R> map3(
            Task<A> first,
            Task<B> second,
            Task<C> third,
            Function3<? super A, ? super B, ? super C, ? extends R> combiner) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        Objects.requireNonNull(combiner, "combiner");
        return () -> {
            try (var scope = StructuredTaskScope.open()) {
                var firstTask = scope.fork(first.asCallable());
                var secondTask = scope.fork(second.asCallable());
                var thirdTask = scope.fork(third.asCallable());
                scope.join();
                return Objects.requireNonNull(
                        combiner.apply(firstTask.get(), secondTask.get(), thirdTask.get()),
                        "map3 result");
            } catch (StructuredTaskScope.FailedException failed) {
                throw failed.getCause();
            }
        };
    }

    @SuppressWarnings("preview")
    public static <A> Task<A> race(List<? extends Task<A>> tasks) {
        Objects.requireNonNull(tasks, "tasks");
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks cannot be empty");
        }
        return () -> {
            try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<A>anySuccessfulResultOrThrow())) {
                for (Task<A> task : tasks) {
                    scope.fork(task.asCallable());
                }
                return scope.join();
            } catch (StructuredTaskScope.FailedException failed) {
                throw failed.getCause();
            }
        };
    }

    @SuppressWarnings("preview")
    public static <A> Task<List<A>> all(List<? extends Task<A>> tasks) {
        Objects.requireNonNull(tasks, "tasks");
        if (tasks.isEmpty()) {
            return Task.pure(List.of());
        }
        return () -> {
            try (var scope = StructuredTaskScope.open()) {
                ArrayList<StructuredTaskScope.Subtask<A>> subtasks = new ArrayList<>(tasks.size());
                for (Task<A> task : tasks) {
                    subtasks.add(scope.fork(task.asCallable()));
                }
                scope.join();
                ArrayList<A> results = new ArrayList<>(subtasks.size());
                for (var subtask : subtasks) {
                    results.add(subtask.get());
                }
                return results;
            } catch (StructuredTaskScope.FailedException failed) {
                throw failed.getCause();
            }
        };
    }

    public static <A, B> Task<List<B>> traverse(
            List<? extends A> values,
            Function<? super A, ? extends Task<B>> mapper) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mapper, "mapper");
        if (values.isEmpty()) {
            return Task.pure(List.of());
        }
        ArrayList<Task<B>> tasks = new ArrayList<>(values.size());
        for (A value : values) {
            tasks.add(Objects.requireNonNull(mapper.apply(value), "traverse task"));
        }
        return all(tasks);
    }
}

package com.flechazo.hkt.business.core;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.control.ListK;
import com.flechazo.hkt.business.control.ValidatedNel;
import com.flechazo.hkt.business.data.NonEmptyList;
import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.util.validation.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.TRAVERSE;

public final class Traverses {
    private Traverses() {
    }

    public static <F extends K1, A, B> App<F, List<B>> traverse(
            Applicative<F, ?> applicative,
            Iterable<? extends A> values,
            Function<? super A, ? extends App<F, B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(applicative, "applicative", TRAVERSE);
        Validation.function().require(f, "f", TRAVERSE);
        App<F, App<ListK.Mu, B>> traversed = ListK.instance().traverse(applicative, f, ListK.from(values));
        return applicative.map(list -> ListK.unbox(list).toList(), traversed);
    }

    public static <A, B> Maybe<List<B>> traverseMaybe(
            Iterable<? extends A> values,
            Function<? super A, Maybe<B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(f, "f", TRAVERSE);
        ArrayList<B> result = new ArrayList<>();
        for (A value : values) {
            Maybe<B> mapped = Validation.function().requireNonNullResult(f.apply(value), "f", TRAVERSE);
            if (mapped.isEmpty()) {
                return Maybe.none();
            }
            result.add(mapped.get());
        }
        return Maybe.some(List.copyOf(result));
    }

    public static <E, A, B> Either<E, List<B>> traverseEither(
            Iterable<? extends A> values,
            Function<? super A, Either<E, B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(f, "f", TRAVERSE);
        ArrayList<B> result = new ArrayList<>();
        for (A value : values) {
            Either<E, B> mapped = Validation.function().requireNonNullResult(f.apply(value), "f", TRAVERSE);
            if (mapped.isLeft()) {
                return Either.left(mapped.left());
            }
            result.add(mapped.right());
        }
        return Either.right(List.copyOf(result));
    }

    public static <E, A, B> Validated<NonEmptyList<E>, List<B>> traverseValidatedNel(
            Iterable<? extends A> values,
            Function<? super A, Validated<NonEmptyList<E>, B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(f, "f", TRAVERSE);
        return Validated.unbox(traverse(ValidatedNel.applicative(), values, f));
    }

    public static <A, B> Task<List<B>> traverseTask(
            Iterable<? extends A> values,
            Function<? super A, Task<B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(f, "f", TRAVERSE);
        Task<List<B>> result = Task.pure(List.of());
        for (A value : values) {
            result = result.flatMap(done -> Validation.function()
                    .requireNonNullResult(f.apply(value), "f", TRAVERSE)
                    .map(next -> {
                ArrayList<B> updated = new ArrayList<>(done.size() + 1);
                updated.addAll(done);
                updated.add(next);
                return List.copyOf(updated);
            }));
        }
        return result;
    }

    public static <A, B> Task<List<B>> parTraverseTask(
            Iterable<? extends A> values,
            Function<? super A, Task<B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(f, "f", TRAVERSE);
        return Task.async(() -> {
            ArrayList<CompletableFuture<B>> futures = new ArrayList<>();
            for (A value : values) {
                futures.add(Validation.function().requireNonNullResult(f.apply(value), "f", TRAVERSE).runAsync());
            }
            CompletableFuture<?>[] array = futures.toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(array).thenApply(ignored -> {
                ArrayList<B> result = new ArrayList<>(futures.size());
                for (CompletableFuture<B> future : futures) {
                    result.add(future.join());
                }
                return List.copyOf(result);
            });
        });
    }
}

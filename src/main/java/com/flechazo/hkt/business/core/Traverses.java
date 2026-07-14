package com.flechazo.hkt.business.core;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.control.ListK;
import com.flechazo.hkt.business.control.ValidatedNel;
import com.flechazo.hkt.business.data.Chain;
import com.flechazo.hkt.business.data.NonEmptyList;
import com.flechazo.hkt.business.effect.VTask;
import com.flechazo.hkt.util.validation.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.TRAVERSE;

/**
 * Traverses iterable values with common applicative effects.
 */
public final class Traverses {
    private Traverses() {
    }

    /**
     * Applies an applicative transformation to each element in encounter order.
     *
     * @param <F> the applicative witness type
     * @param <A> the source element type
     * @param <B> the result element type
     * @param applicative the applicative used to combine effects
     * @param values the source elements
     * @param f the effectful element transformation
     * @return the collected unmodifiable list in the applicative context
     */
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

    /**
     * Applies a maybe-producing transformation until an empty result is encountered.
     *
     * @param <A> the source element type
     * @param <B> the result element type
     * @param values the source elements
     * @param f the element transformation
     * @return a defined unmodifiable list when every transformation succeeds, otherwise an empty value
     */
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
        return Maybe.some(Collections.unmodifiableList(result));
    }

    /**
     * Applies an either-producing transformation until a left result is encountered.
     *
     * @param <E> the error type
     * @param <A> the source element type
     * @param <B> the result element type
     * @param values the source elements
     * @param f the element transformation
     * @return the first left error or a right unmodifiable list of results
     */
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
        return Either.right(Collections.unmodifiableList(result));
    }

    /**
     * Applies a validation transformation and accumulates all errors.
     *
     * @param <E> the validation error element type
     * @param <A> the source element type
     * @param <B> the result element type
     * @param values the source elements
     * @param f the validation transformation
     * @return the accumulated non-empty errors or an unmodifiable list of results
     */
    public static <E, A, B> Validated<NonEmptyList<E>, List<B>> traverseValidatedNel(
            Iterable<? extends A> values,
            Function<? super A, Validated<NonEmptyList<E>, B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(f, "f", TRAVERSE);
        return Validated.unbox(traverse(ValidatedNel.applicative(), values, f));
    }

    /**
     * Creates a task that executes element tasks sequentially in encounter order.
     *
     * @param <A> the source element type
     * @param <B> the result element type
     * @param values the source elements
     * @param f the task-producing transformation
     * @return a task producing an unmodifiable list of results
     */
    public static <A, B> VTask<List<B>> traverseVTask(
            Iterable<? extends A> values,
            Function<? super A, VTask<B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(f, "f", TRAVERSE);
        return () -> {
            Chain<B> result = Chain.empty();
            for (A value : values) {
                VTask<B> next = Validation.function().requireNonNullResult(f.apply(value), "f", TRAVERSE);
                result = result.append(next.execute());
            }
            return result.toList();
        };
    }

    /**
     * Creates a task that starts element tasks concurrently and preserves result order.
     *
     * @param <A> the source element type
     * @param <B> the result element type
     * @param values the source elements
     * @param f the task-producing transformation
     * @return a task producing an unmodifiable list of results in source order
     */
    public static <A, B> VTask<List<B>> parTraverseVTask(
            Iterable<? extends A> values,
            Function<? super A, VTask<B>> f) {
        Objects.requireNonNull(values, "values");
        Validation.function().require(f, "f", TRAVERSE);
        return VTask.async(() -> {
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
                return Collections.unmodifiableList(result);
            });
        });
    }
}

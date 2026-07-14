package com.flechazo.hkt.business.control;


import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.combinable.Combinable;
import com.flechazo.hkt.business.capability.combinable.ListCombinable;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.effect.IO;
import com.flechazo.hkt.business.effect.IOPath;

import java.util.*;
import java.util.function.*;

/**
 * Provides fluent composition over an immutable list of values.
 *
 * @param <A> the element type
 */
public final class ListPath<A> implements Chainable<A>, ListCombinable<A> {
    private final List<A> values;

    /**
     * Creates a path from elements in encounter order.
     *
     * @param values the source elements
     */
    public ListPath(List<? extends A> values) {
        this.values = Collections.unmodifiableList(Objects.requireNonNull(values, "values"));
    }

    /**
     * Returns the values in encounter order.
     *
     * @return the unmodifiable list represented by this path
     */
    public List<A> run() {
        return values;
    }

    /**
     * Returns the first element when present.
     *
     * @return a defined path containing the first element, or an empty path
     */
    public MaybePath<A> head() {
        return values.isEmpty() ? Pathway.nothing() : Pathway.just(values.getFirst());
    }

    /**
     * Returns the last element when present.
     *
     * @return a defined path containing the last element, or an empty path
     */
    public MaybePath<A> last() {
        return values.isEmpty() ? Pathway.nothing() : Pathway.just(values.getLast());
    }

    /**
     * Returns the element at an index when the index is valid.
     *
     * @param index the zero-based index
     * @return a defined path containing the element, or an empty path for an invalid index
     */
    public MaybePath<A> get(int index) {
        return index >= 0 && index < values.size() ? Pathway.just(values.get(index)) : Pathway.nothing();
    }

    /**
     * Determines whether this path contains no elements.
     *
     * @return {@code true} when the list is empty
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns the number of elements.
     *
     * @return the element count
     */
    public int size() {
        return values.size();
    }

    /**
     * Determines whether any element satisfies a predicate.
     *
     * @param predicate the condition to test
     * @return {@code true} when at least one element matches
     */
    public boolean anyMatch(Predicate<? super A> predicate) {
        return values.stream().anyMatch(predicate);
    }

    /**
     * Determines whether every element satisfies a predicate.
     *
     * @param predicate the condition to test
     * @return {@code true} when every element matches, including for an empty path
     */
    public boolean allMatch(Predicate<? super A> predicate) {
        return values.stream().allMatch(predicate);
    }

    @Override
    public <B> ListPath<B> map(Function<? super A, ? extends B> mapper) {
        ArrayList<B> result = new ArrayList<>(values.size());
        for (A value : values) {
            result.add(mapper.apply(value));
        }
        return new ListPath<>(result);
    }

    @Override
    public ListPath<A> peek(Consumer<? super A> consumer) {
        values.forEach(consumer);
        return this;
    }

    /**
     * Combines list elements positionally until either list is exhausted.
     *
     * @param <B> the other element type
     * @param <C> the combined element type
     * @param other the list path to combine with this path
     * @param combiner the function combining corresponding elements
     * @return the positionally combined list path
     * @throws IllegalArgumentException if {@code other} is not a list path
     */
    @Override
    public <B, C> ListPath<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof ListPath<?> otherList)) {
            throw new IllegalArgumentException("Cannot zipWith non-ListPath: " + other.getClass());
        }
        ListPath<B> typedOther = (ListPath<B>) otherList;
        int size = Math.min(values.size(), typedOther.values.size());
        ArrayList<C> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(combiner.apply(values.get(i), typedOther.values.get(i)));
        }
        return new ListPath<>(result);
    }

    @Override
    public <B> ListPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        ArrayList<B> result = new ArrayList<>();
        for (A value : values) {
            Chainable<B> mapped = mapper.apply(value);
            if (!(mapped instanceof ListPath<?> listPath)) {
                throw new IllegalArgumentException("via mapper must return ListPath, got: " + mapped.getClass());
            }
            result.addAll(((ListPath<B>) listPath).values);
        }
        return new ListPath<>(result);
    }

    @Override
    public <B> ListPath<B> then(Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    /**
     * Retains elements satisfying a predicate.
     *
     * @param predicate the condition for retaining an element
     * @return a path of matching elements in encounter order
     */
    public ListPath<A> filter(Predicate<? super A> predicate) {
        ArrayList<A> result = new ArrayList<>();
        for (A value : values) {
            if (predicate.test(value)) {
                result.add(value);
            }
        }
        return new ListPath<>(result);
    }

    /**
     * Retains at most the first requested number of elements.
     *
     * @param n the maximum number of elements to retain
     * @return a path containing the retained prefix
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public ListPath<A> take(int n) {
        return new ListPath<>(values.subList(0, Math.clamp(n, 0, values.size())));
    }

    /**
     * Removes at most the first requested number of elements.
     *
     * @param n the maximum number of elements to remove
     * @return a path containing the remaining suffix
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public ListPath<A> drop(int n) {
        return new ListPath<>(values.subList(Math.clamp(n, 0, values.size()), values.size()));
    }

    /**
     * Retains the longest prefix whose elements satisfy a predicate.
     *
     * @param predicate the prefix condition
     * @return a path containing the matching prefix
     */
    public ListPath<A> takeWhile(Predicate<? super A> predicate) {
        ArrayList<A> result = new ArrayList<>();
        for (A value : values) {
            if (!predicate.test(value)) {
                break;
            }
            result.add(value);
        }
        return new ListPath<>(result);
    }

    /**
     * Removes the longest prefix whose elements satisfy a predicate.
     *
     * @param predicate the prefix condition
     * @return a path containing the remaining suffix
     */
    public ListPath<A> dropWhile(Predicate<? super A> predicate) {
        int index = 0;
        while (index < values.size() && predicate.test(values.get(index))) {
            index++;
        }
        return new ListPath<>(values.subList(index, values.size()));
    }

    /**
     * Removes duplicate elements while preserving first encounter order.
     *
     * @return a path containing distinct elements
     */
    public ListPath<A> distinct() {
        return new ListPath<>(new ArrayList<>(new LinkedHashSet<>(values)));
    }

    /**
     * Concatenates this path with another list path.
     *
     * @param other the path whose elements follow this path
     * @return the concatenated path
     */
    public ListPath<A> concat(ListPath<A> other) {
        ArrayList<A> result = new ArrayList<>(values.size() + other.values.size());
        result.addAll(values);
        result.addAll(other.values);
        return new ListPath<>(result);
    }

    /**
     * Folds elements from first to last.
     *
     * @param <B> the accumulated value type
     * @param initial the initial accumulated value
     * @param f the operation combining the accumulated value and next element
     * @return the final accumulated value
     */
    public <B> B foldLeft(B initial, BiFunction<B, A, B> f) {
        B result = initial;
        for (A value : values) {
            result = f.apply(result, value);
        }
        return result;
    }

    /**
     * Folds elements from last to first.
     *
     * @param <B> the accumulated value type
     * @param initial the initial accumulated value
     * @param f the operation combining the next element and accumulated value
     * @return the final accumulated value
     */
    public <B> B foldRight(B initial, BiFunction<A, B, B> f) {
        B result = initial;
        for (int i = values.size() - 1; i >= 0; i--) {
            result = f.apply(values.get(i), result);
        }
        return result;
    }

    /**
     * Folds elements with a binary operation.
     *
     * @param identity the result used for an empty path
     * @param op the element combination operation
     * @return the folded value
     */
    public A fold(A identity, BinaryOperator<A> op) {
        return foldLeft(identity, op);
    }

    /**
     * Maps elements to a monoid and combines them in encounter order.
     *
     * @param <M> the accumulated value type
     * @param monoid the monoid used to combine mapped values
     * @param f the element mapping function
     * @return the combined mapped value
     */
    public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f) {
        return foldLeft(monoid.empty(), (acc, value) -> monoid.combine(acc, f.apply(value)));
    }

    /**
     * Reverses element encounter order.
     *
     * @return the reversed list path
     */
    public ListPath<A> reverse() {
        ArrayList<A> result = new ArrayList<>(values);
        Collections.reverse(result);
        return new ListPath<>(result);
    }

    /**
     * Returns the first element as a maybe path.
     *
     * @return the first element, or an empty path
     */
    public MaybePath<A> toMaybePath() {
        return head();
    }

    /**
     * Lifts the entire list into an IO path.
     *
     * @return an IO path containing the unmodifiable list
     */
    public IOPath<List<A>> toIOPath() {
        return new IOPath<>(IO.pure(values));
    }

}

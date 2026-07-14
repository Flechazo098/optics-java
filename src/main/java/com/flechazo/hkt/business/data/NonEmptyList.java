package com.flechazo.hkt.business.data;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Semigroup;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;

/**
 * Represents an immutable ordered list containing at least one non-null element.
 *
 * @param <A> the element type
 */
public final class NonEmptyList<A> implements Iterable<A> {
    private final A head;
    private final List<A> tail;
    private final Chain<A> appended;

    private NonEmptyList(A head, List<A> tail, Chain<A> appended) {
        this.head = Objects.requireNonNull(head, "head");
        this.tail = Objects.requireNonNull(tail, "tail");
        this.appended = Objects.requireNonNull(appended, "appended");
    }

    /**
     * Creates a non-empty list containing one element.
     *
     * @param <A> the element type
     * @param head the first and sole element
     * @return a singleton non-empty list
     */
    public static <A> NonEmptyList<A> of(A head) {
        return new NonEmptyList<>(head, List.of(), Chain.empty());
    }

    /**
     * Creates a non-empty list from a head and tail.
     *
     * @param <A> the element type
     * @param head the first element
     * @param tail the remaining elements in encounter order
     * @return a non-empty list containing {@code head} followed by {@code tail}
     */
    public static <A> NonEmptyList<A> of(A head, List<? extends A> tail) {
        return fromTail(head, tail);
    }

    /**
     * Creates a non-empty list when the supplied list contains at least one element.
     *
     * @param <A> the element type
     * @param values the source elements
     * @return a defined non-empty list, or an empty value when {@code values} is empty
     */
    public static <A> Maybe<NonEmptyList<A>> fromList(List<? extends A> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return Maybe.none();
        }
        return Maybe.some(fromTail(values.getFirst(), values.subList(1, values.size())));
    }

    /**
     * Returns the concatenation semigroup for non-empty lists.
     *
     * @param <A> the element type
     * @return a semigroup that concatenates its operands
     */
    public static <A> Semigroup<NonEmptyList<A>> semigroup() {
        return NonEmptyList::appendAll;
    }

    /**
     * Returns the first element.
     *
     * @return the first element
     */
    public A head() {
        return head;
    }

    /**
     * Returns all elements after the first element.
     *
     * @return an unmodifiable list containing the tail
     */
    public List<A> tail() {
        if (appended.isEmpty()) {
            return tail;
        }
        ArrayList<A> result = new ArrayList<>(tail.size() + appended.size());
        result.addAll(tail);
        result.addAll(appended.toList());
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns all elements as an unmodifiable list.
     *
     * @return an unmodifiable list in encounter order
     */
    public List<A> toList() {
        ArrayList<A> result = new ArrayList<>(size());
        result.add(head);
        result.addAll(tail);
        result.addAll(appended.toList());
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the number of elements.
     *
     * @return a positive element count
     */
    public int size() {
        return tail.size() + appended.size() + 1;
    }

    /**
     * Returns a non-empty list with an element added at the end.
     *
     * @param value the element to append
     * @return the extended non-empty list
     */
    public NonEmptyList<A> append(A value) {
        return new NonEmptyList<>(
                head,
                tail,
                appended.append(Objects.requireNonNull(value, "value")));
    }

    /**
     * Concatenates this list with another non-empty list.
     *
     * @param other the list whose elements follow this list
     * @return the concatenated non-empty list
     */
    public NonEmptyList<A> appendAll(NonEmptyList<? extends A> other) {
        Objects.requireNonNull(other, "other");
        Chain<A> next = appended;
        for (A value : other) {
            next = next.append(value);
        }
        return new NonEmptyList<>(head, tail, next);
    }

    /**
     * Transforms every element while preserving encounter order and non-emptiness.
     *
     * @param <B> the result element type
     * @param f the element transformation
     * @return a non-empty list of transformed elements
     */
    public <B> NonEmptyList<B> map(Function<? super A, ? extends B> f) {
        Objects.requireNonNull(f, "f");
        B nextHead = Objects.requireNonNull(f.apply(head), "mapped head");
        ArrayList<B> nextTail = new ArrayList<>(size() - 1);
        for (A value : tail()) {
            nextTail.add(Objects.requireNonNull(f.apply(value), "mapped tail value"));
        }
        return new NonEmptyList<>(nextHead, Collections.unmodifiableList(nextTail), Chain.empty());
    }

    @Override
    @NonNull
    public Iterator<A> iterator() {
        return new Iterator<>() {
            private boolean atHead = true;
            private final Iterator<A> tailIterator = tail().iterator();

            @Override
            public boolean hasNext() {
                return atHead || tailIterator.hasNext();
            }

            @Override
            public A next() {
                if (atHead) {
                    atHead = false;
                    return head;
                }
                return tailIterator.next();
            }
        };
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    @SuppressWarnings("unchecked")
    private static <A> List<A> narrow(List<? extends A> values) {
        return (List<A>) Objects.requireNonNull(values, "values");
    }

    private static <A> NonEmptyList<A> fromTail(A head, List<? extends A> tail) {
        List<A> values = Collections.unmodifiableList(narrow(tail));
        for (A value : values) {
            Objects.requireNonNull(value, "tail value");
        }
        return new NonEmptyList<>(head, values, Chain.empty());
    }
}
